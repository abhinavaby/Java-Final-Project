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
CREATE POLICY "Public profiles are readable by authenticated users"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (auth.uid() = id);

-- 2. Conversations Policies
CREATE POLICY "Users can view conversations they belong to or public channels"
    ON public.conversations FOR SELECT
    TO authenticated
    USING (
        is_private = false OR
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = conversations.id
            AND cm.user_id = auth.uid()
        )
    );

CREATE POLICY "Authenticated users can create conversations"
    ON public.conversations FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "Owners and admins can update conversations"
    ON public.conversations FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = conversations.id
            AND cm.user_id = auth.uid()
            AND cm.role IN ('OWNER', 'ADMIN')
        )
    );

-- 3. Conversation Members Policies
CREATE POLICY "Members can view members of their conversations"
    ON public.conversation_members FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = conversation_members.conversation_id
            AND cm.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert themselves or admins can add members"
    ON public.conversation_members FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = conversation_members.conversation_id
            AND cm.user_id = auth.uid()
            AND cm.role IN ('OWNER', 'ADMIN')
        )
    );

CREATE POLICY "Users can update their own membership state or admins can update roles"
    ON public.conversation_members FOR UPDATE
    TO authenticated
    USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = conversation_members.conversation_id
            AND cm.user_id = auth.uid()
            AND cm.role IN ('OWNER', 'ADMIN')
        )
    );

CREATE POLICY "Users can leave or admins can remove members"
    ON public.conversation_members FOR DELETE
    TO authenticated
    USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.conversation_members cm
            WHERE cm.conversation_id = conversation_members.conversation_id
            AND cm.user_id = auth.uid()
            AND cm.role IN ('OWNER', 'ADMIN')
        )
    );

-- 4. Messages Policies
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

CREATE POLICY "Senders can edit or soft-delete their own messages"
    ON public.messages FOR UPDATE
    TO authenticated
    USING (auth.uid() = sender_id);

-- 5. Attachments Policies
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

CREATE POLICY "Users can manage their own receipts"
    ON public.message_receipts FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 7. Message Reactions Policies
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

CREATE POLICY "Users can manage their own reactions"
    ON public.message_reactions FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 8. Polls Policies
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

CREATE POLICY "Members can cast and delete their own votes"
    ON public.poll_votes FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 10. Blocked Users Policies
CREATE POLICY "Users can manage their block list"
    ON public.blocked_users FOR ALL
    TO authenticated
    USING (blocker_id = auth.uid())
    WITH CHECK (blocker_id = auth.uid());
