-- ConnectAI v2 Supabase setup (single-file edition)
-- Run this in Supabase Dashboard > SQL Editor.
-- Sections 1-5 install the app schema, access policies, realtime, polls, and storage.
-- Existing policies with the same names are dropped and recreated with these definitions.
-- Section 6 contains optional read-only verification queries.
-- The OpenAI API key is not SQL: set it as an Edge Function secret separately.

-- ============================================================================
-- 1. Core schema and Realtime publication
-- Source: schema.sql
-- ============================================================================

-- ConnectAI Supabase PostgreSQL Schema
-- Database tables, constraints, triggers, and performance indexes

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Profiles Table (Linked to auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_path TEXT,
    about TEXT DEFAULT 'Hey there! I am using ConnectAI.',
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Conversations Table
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(100),
    description TEXT,
    image_path TEXT,
    created_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    invite_code VARCHAR(32) UNIQUE,
    is_private BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Conversation Members Table
CREATE TABLE IF NOT EXISTS public.conversation_members (
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_muted BOOLEAN DEFAULT false,
    is_archived BOOLEAN DEFAULT false,
    is_pinned BOOLEAN DEFAULT false,
    last_read_message_id UUID,
    PRIMARY KEY (conversation_id, user_id)
);

-- 4. Messages Table
CREATE TABLE IF NOT EXISTS public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'DOCUMENT', 'AUDIO', 'VIDEO', 'VOICE', 'POLL', 'SYSTEM')),
    content TEXT,
    reply_to_message_id UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 5. Attachments Table
CREATE TABLE IF NOT EXISTS public.attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    duration_seconds INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Message Receipts Table
CREATE TABLE IF NOT EXISTS public.message_receipts (
    message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (message_id, user_id)
);

-- 7. Message Reactions Table
CREATE TABLE IF NOT EXISTS public.message_reactions (
    message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    emoji VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id, emoji)
);

-- 8. Polls Table
CREATE TABLE IF NOT EXISTS public.polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    created_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    question TEXT NOT NULL,
    allows_multiple_answers BOOLEAN DEFAULT false,
    is_anonymous BOOLEAN DEFAULT false,
    closes_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 9. Poll Options Table
CREATE TABLE IF NOT EXISTS public.poll_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES public.polls(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    position INT NOT NULL
);

-- 10. Poll Votes Table
CREATE TABLE IF NOT EXISTS public.poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES public.polls(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES public.poll_options(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(poll_id, option_id, user_id)
);

-- 11. Blocked Users Table
CREATE TABLE IF NOT EXISTS public.blocked_users (
    blocker_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    blocked_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (blocker_id, blocked_id)
);

-- Indexes for performance query optimization
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON public.messages(conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversation_members_user ON public.conversation_members(user_id);
CREATE INDEX IF NOT EXISTS idx_message_receipts_user_read ON public.message_receipts(user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles(username);
CREATE INDEX IF NOT EXISTS idx_poll_options_poll ON public.poll_options(poll_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_poll ON public.poll_votes(poll_id);
CREATE INDEX IF NOT EXISTS idx_attachments_message ON public.attachments(message_id);

-- Publish chat events to Supabase Realtime for clients without the Supabase JS SDK.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'messages') THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'message_reactions') THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.message_reactions;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'poll_votes') THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.poll_votes;
        END IF;
    END IF;
END $$;

-- Automatic Profile Creation & Default Channel Enrollment on Auth Sign Up
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    -- 1. Create public profile
    INSERT INTO public.profiles (id, username, display_name, avatar_path, about)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', CONCAT('user_', SUBSTRING(NEW.id::text FROM 1 FOR 8))),
        COALESCE(NEW.raw_user_meta_data->>'display_name', 'ConnectAI User'),
        NEW.raw_user_meta_data->>'avatar_path',
        'Hey there! I am using ConnectAI.'
    )
    ON CONFLICT (id) DO NOTHING;

    -- 2. Automatically grant membership to the three public default channels (accessible to everyone)
    INSERT INTO public.conversation_members (conversation_id, user_id, role)
    VALUES
        ('00000000-0000-0000-0000-000000000001', NEW.id, 'MEMBER'),
        ('00000000-0000-0000-0000-000000000002', NEW.id, 'MEMBER'),
        ('00000000-0000-0000-0000-000000000003', NEW.id, 'MEMBER')
    ON CONFLICT (conversation_id, user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Automatic Conversation Updated_At Trigger on Message
CREATE OR REPLACE FUNCTION public.update_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.conversations
    SET updated_at = NOW()
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_new_message_update_conversation ON public.messages;
CREATE TRIGGER on_new_message_update_conversation
    AFTER INSERT ON public.messages
    FOR EACH ROW EXECUTE FUNCTION public.update_conversation_timestamp();

-- Seed the 3 Default Public Channels with access to everyone
INSERT INTO public.conversations (id, type, name, description, invite_code, is_private)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'GROUP', 'General Lounge', 'Public discussion lounge for all team members and newcomers.', 'GENERAL', false),
    ('00000000-0000-0000-0000-000000000002', 'GROUP', 'Tech & AI Product Team', 'Core architecture, Java Swing UI components & AI integrations.', 'TECH2026', false),
    ('00000000-0000-0000-0000-000000000003', 'GROUP', 'Design System Guild', 'UI/UX design, visual tokens, layouts & desktop guidelines.', 'DESIGN2026', false)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_private = EXCLUDED.is_private;

-- Backfill: Ensure all existing registered profiles are enrolled in the 3 default channels
INSERT INTO public.conversation_members (conversation_id, user_id, role)
SELECT c.id, p.id, 'MEMBER'
FROM public.conversations c
CROSS JOIN public.profiles p
WHERE c.id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'
)
ON CONFLICT (conversation_id, user_id) DO NOTHING;

-- ============================================================================
-- 2. Row Level Security policies
-- Source: rls-policies.sql
-- ============================================================================

-- ConnectAI Row Level Security (RLS) Policies

-- Enable RLS on all user-data tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.polls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.poll_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.poll_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocked_users ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policies
DROP POLICY IF EXISTS "Public profiles are readable by authenticated users" ON public.profiles;
CREATE POLICY "Public profiles are readable by authenticated users"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (auth.uid() = id);

-- 2. Conversations Policies
-- Avoid recursive RLS queries by checking membership through a narrowly scoped helper.
CREATE OR REPLACE FUNCTION public.is_conversation_member(target_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT EXISTS (SELECT 1 FROM public.conversation_members
        WHERE conversation_id = target_id AND user_id = auth.uid());
$$;
CREATE OR REPLACE FUNCTION public.is_conversation_admin(target_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT EXISTS (SELECT 1 FROM public.conversation_members
        WHERE conversation_id = target_id AND user_id = auth.uid() AND role IN ('OWNER', 'ADMIN'));
$$;
REVOKE ALL ON FUNCTION public.is_conversation_member(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.is_conversation_admin(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_conversation_member(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_conversation_admin(uuid) TO authenticated;

DROP POLICY IF EXISTS "Users can view conversations they belong to or public channels" ON public.conversations;
CREATE POLICY "Users can view conversations they belong to or public channels"
    ON public.conversations FOR SELECT
    TO authenticated
    USING (
        created_by = auth.uid() OR is_private = false OR
        public.is_conversation_member(conversations.id)
    );

DROP POLICY IF EXISTS "Authenticated users can create conversations" ON public.conversations;
CREATE POLICY "Authenticated users can create conversations"
    ON public.conversations FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = created_by);

DROP POLICY IF EXISTS "Owners and admins can update conversations" ON public.conversations;
CREATE POLICY "Owners and admins can update conversations"
    ON public.conversations FOR UPDATE
    TO authenticated
    USING (
        public.is_conversation_admin(conversations.id)
    );

-- 3. Conversation Members Policies
DROP POLICY IF EXISTS "Members can view members of their conversations" ON public.conversation_members;
CREATE POLICY "Members can view members of their conversations"
    ON public.conversation_members FOR SELECT
    TO authenticated
    USING (
        public.is_conversation_member(conversation_members.conversation_id)
    );

DROP POLICY IF EXISTS "Users can insert themselves or admins can add members" ON public.conversation_members;
CREATE POLICY "Users can insert themselves or admins can add members"
    ON public.conversation_members FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid() OR
        public.is_conversation_admin(conversation_members.conversation_id)
    );

DROP POLICY IF EXISTS "Users can update their own membership state or admins can update roles" ON public.conversation_members;
CREATE POLICY "Users can update their own membership state or admins can update roles"
    ON public.conversation_members FOR UPDATE
    TO authenticated
    USING (
        user_id = auth.uid() OR
        public.is_conversation_admin(conversation_members.conversation_id)
    );

DROP POLICY IF EXISTS "Users can leave or admins can remove members" ON public.conversation_members;
CREATE POLICY "Users can leave or admins can remove members"
    ON public.conversation_members FOR DELETE
    TO authenticated
    USING (
        user_id = auth.uid() OR
        public.is_conversation_admin(conversation_members.conversation_id)
    );

-- 4. Messages Policies
DROP POLICY IF EXISTS "Members can view messages in their conversations" ON public.messages;
CREATE POLICY "Members can view messages in their conversations"
    ON public.messages FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = messages.conversation_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Members can insert messages in their conversations" ON public.messages;
CREATE POLICY "Members can insert messages in their conversations"
    ON public.messages FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = sender_id AND
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = messages.conversation_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Senders can edit or soft-delete their own messages" ON public.messages;
CREATE POLICY "Senders can edit or soft-delete their own messages"
    ON public.messages FOR UPDATE
    TO authenticated
    USING (auth.uid() = sender_id);

-- 5. Attachments Policies
DROP POLICY IF EXISTS "Members can view attachments for their conversation messages" ON public.attachments;
CREATE POLICY "Members can view attachments for their conversation messages"
    ON public.attachments FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.messages m
            JOIN public.conversation_members cm ON cm.conversation_id = m.conversation_id
            WHERE m.id = attachments.message_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Senders can insert attachments for their messages" ON public.attachments;
CREATE POLICY "Senders can insert attachments for their messages"
    ON public.attachments FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.messages m
            WHERE m.id = attachments.message_id
            AND m.sender_id = auth.uid()
        )
    );

-- 6. Message Receipts Policies
DROP POLICY IF EXISTS "Members can view receipts" ON public.message_receipts;
CREATE POLICY "Members can view receipts"
    ON public.message_receipts FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.messages m
            JOIN public.conversation_members cm ON cm.conversation_id = m.conversation_id
            WHERE m.id = message_receipts.message_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Users can manage their own receipts" ON public.message_receipts;
CREATE POLICY "Users can manage their own receipts"
    ON public.message_receipts FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 7. Message Reactions Policies
DROP POLICY IF EXISTS "Members can view reactions" ON public.message_reactions;
CREATE POLICY "Members can view reactions"
    ON public.message_reactions FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.messages m
            JOIN public.conversation_members cm ON cm.conversation_id = m.conversation_id
            WHERE m.id = message_reactions.message_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Users can manage their own reactions" ON public.message_reactions;
CREATE POLICY "Users can manage their own reactions"
    ON public.message_reactions FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 8. Polls Policies
DROP POLICY IF EXISTS "Members can view polls in their conversations" ON public.polls;
CREATE POLICY "Members can view polls in their conversations"
    ON public.polls FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = polls.conversation_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Members can create polls in their conversations" ON public.polls;
CREATE POLICY "Members can create polls in their conversations"
    ON public.polls FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = created_by AND
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = polls.conversation_id
            AND cm.user_id = auth.uid()
        )
    );

-- 9. Poll Options & Votes Policies
DROP POLICY IF EXISTS "Members can view poll options" ON public.poll_options;
CREATE POLICY "Members can view poll options"
    ON public.poll_options FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.polls p
            JOIN public.conversation_members cm ON cm.conversation_id = p.conversation_id
            WHERE p.id = poll_options.poll_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Poll creators can insert poll options" ON public.poll_options;
CREATE POLICY "Poll creators can insert poll options"
    ON public.poll_options FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.polls p
            WHERE p.id = poll_options.poll_id
            AND p.created_by = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Members can view poll votes" ON public.poll_votes;
CREATE POLICY "Members can view poll votes"
    ON public.poll_votes FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.polls p
            JOIN public.conversation_members cm ON cm.conversation_id = p.conversation_id
            WHERE p.id = poll_votes.poll_id
            AND cm.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Members can cast and delete their own votes" ON public.poll_votes;
CREATE POLICY "Members can cast and delete their own votes"
    ON public.poll_votes FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 10. Blocked Users Policies
DROP POLICY IF EXISTS "Users can manage their block list" ON public.blocked_users;
CREATE POLICY "Users can manage their block list"
    ON public.blocked_users FOR ALL
    TO authenticated
    USING (blocker_id = auth.uid())
    WITH CHECK (blocker_id = auth.uid());

-- ============================================================================
-- 3. Group membership role protections
-- Source: secure-group-membership-roles.sql
-- ============================================================================

-- Tighten group role changes and member removal to match owner/admin permissions.
-- Run after rls-policies.sql on an existing Supabase project.

CREATE OR REPLACE FUNCTION public.is_conversation_owner(target_id uuid)
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.conversation_members
        WHERE conversation_id = target_id AND user_id = auth.uid() AND role = 'OWNER'
    );
$$;

CREATE OR REPLACE FUNCTION public.can_manage_conversation_member(target_id uuid, target_user uuid)
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.conversation_members target
        JOIN public.conversation_members actor ON actor.conversation_id = target.conversation_id
        WHERE target.conversation_id = target_id
          AND target.user_id = target_user
          AND target.role <> 'OWNER'
          AND actor.user_id = auth.uid()
          AND (
              actor.role = 'OWNER'
              OR (actor.role = 'ADMIN' AND target.role = 'MEMBER')
          )
    );
$$;

REVOKE ALL ON FUNCTION public.is_conversation_owner(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.can_manage_conversation_member(uuid, uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_conversation_owner(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.can_manage_conversation_member(uuid, uuid) TO authenticated;

CREATE OR REPLACE FUNCTION public.protect_group_member_role()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role THEN
        IF OLD.role = 'OWNER' OR NOT public.can_manage_conversation_member(OLD.conversation_id, OLD.user_id) THEN
            RAISE EXCEPTION 'You do not have permission to change this member role' USING ERRCODE = '42501';
        END IF;
        IF NOT public.is_conversation_owner(OLD.conversation_id) AND NEW.role <> 'MEMBER' THEN
            RAISE EXCEPTION 'Admins cannot promote members or assign the owner role' USING ERRCODE = '42501';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS protect_group_member_role ON public.conversation_members;
CREATE TRIGGER protect_group_member_role
    BEFORE UPDATE OF role ON public.conversation_members
    FOR EACH ROW EXECUTE FUNCTION public.protect_group_member_role();

DROP POLICY IF EXISTS "Users can update their own membership state or admins can update roles" ON public.conversation_members;
DROP POLICY IF EXISTS "Members can update own preferences or authorized roles" ON public.conversation_members;
CREATE POLICY "Members can update own preferences or authorized roles"
    ON public.conversation_members FOR UPDATE TO authenticated
    USING (
        user_id = auth.uid()
        OR public.can_manage_conversation_member(conversation_id, user_id)
    )
    WITH CHECK (
        user_id = auth.uid()
        OR (
            public.is_conversation_admin(conversation_id)
            AND role IN ('ADMIN', 'MEMBER')
            AND (public.is_conversation_owner(conversation_id) OR role = 'MEMBER')
        )
    );

DROP POLICY IF EXISTS "Users can leave or admins can remove members" ON public.conversation_members;
DROP POLICY IF EXISTS "Users can leave or authorized group managers can remove members" ON public.conversation_members;
CREATE POLICY "Users can leave or authorized group managers can remove members"
    ON public.conversation_members FOR DELETE TO authenticated
    USING (
        (user_id = auth.uid() AND role <> 'OWNER')
        OR public.can_manage_conversation_member(conversation_id, user_id)
    );

-- ============================================================================
-- 4. Private poll result summary function
-- Source: poll-results.sql
-- ============================================================================

-- Return vote totals and only the current user's selections. This keeps
-- anonymous poll votes private while allowing clients to render live results.
CREATE OR REPLACE FUNCTION public.get_poll_results(p_poll_id uuid)
RETURNS TABLE (
    option_id uuid,
    vote_count bigint,
    total_voters bigint,
    total_votes bigint,
    has_voted boolean
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    target_conversation uuid;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Sign in to view poll results' USING ERRCODE = '42501';
    END IF;

    SELECT p.conversation_id INTO target_conversation
    FROM public.polls p WHERE p.id = p_poll_id;

    IF target_conversation IS NULL OR NOT public.is_conversation_member(target_conversation) THEN
        RAISE EXCEPTION 'You cannot view this poll' USING ERRCODE = '42501';
    END IF;

    RETURN QUERY
    WITH scoped_votes AS (
        SELECT v.option_id, v.user_id
        FROM public.poll_votes v
        WHERE v.poll_id = p_poll_id
    ), totals AS (
        SELECT count(*)::bigint AS vote_total, count(DISTINCT user_id)::bigint AS voter_total
        FROM scoped_votes
    )
    SELECT o.id,
           count(v.option_id)::bigint,
           totals.voter_total,
           totals.vote_total,
           coalesce(bool_or(v.user_id = auth.uid()), false)
    FROM public.poll_options o
    CROSS JOIN totals
    LEFT JOIN scoped_votes v ON v.option_id = o.id
    WHERE o.poll_id = p_poll_id
    GROUP BY o.id, o.position, totals.voter_total, totals.vote_total
    ORDER BY o.position;
END;
$$;

REVOKE ALL ON FUNCTION public.get_poll_results(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_poll_results(uuid) TO authenticated;

-- ============================================================================
-- 5. Chat attachment storage bucket and policies
-- Source: storage-policies.sql
-- ============================================================================

-- ConnectAI Supabase Storage Policies
-- Storage bucket name: chat-attachments

-- Create private bucket 'chat-attachments' if not already created via dashboard
INSERT INTO storage.buckets (id, name, public)
VALUES ('chat-attachments', 'chat-attachments', false)
ON CONFLICT (id) DO NOTHING;

-- Policy 1: Authenticated users can upload attachments to chat-attachments bucket
DROP POLICY IF EXISTS "Authenticated users can upload attachments" ON storage.objects;
CREATE POLICY "Authenticated users can upload attachments"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'chat-attachments');

-- Policy 2: Users can download/read objects if they are authenticated
DROP POLICY IF EXISTS "Authenticated users can read attachments" ON storage.objects;
CREATE POLICY "Authenticated users can read attachments"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'chat-attachments');

-- Policy 3: Users can delete their own uploaded storage objects
DROP POLICY IF EXISTS "Users can delete own storage objects" ON storage.objects;
CREATE POLICY "Users can delete own storage objects"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'chat-attachments' AND auth.uid() = owner);

-- ============================================================================
-- 6. Optional verification queries
-- Source: sample-data.sql
-- ============================================================================

-- ============================================================================
-- ConnectAI Public Channels & Verification Script
-- ============================================================================

-- 1. Check the 3 default public channels
SELECT id, name, description, invite_code, is_private
FROM public.conversations
ORDER BY created_at ASC;

-- 2. Check registered profiles
SELECT id, username, display_name, created_at
FROM public.profiles
ORDER BY created_at DESC;

-- 3. Check channel memberships (All registered users should belong to all 3 channels)
SELECT c.name AS channel_name, p.display_name AS member_name, cm.role
FROM public.conversation_members cm
JOIN public.conversations c ON c.id = cm.conversation_id
JOIN public.profiles p ON p.id = cm.user_id
ORDER BY c.name, p.display_name;
