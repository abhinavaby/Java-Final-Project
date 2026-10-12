-- Run once in the Supabase SQL editor for an existing ConnectAI database.
BEGIN;
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


-- Restore explicit Data API privileges without granting anonymous access.
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.profiles TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.conversations TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.conversation_members TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.messages TO authenticated;
GRANT SELECT ON public.attachments, public.polls, public.poll_options, public.message_reactions TO authenticated;

DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
CREATE POLICY "Users can insert their own profile" ON public.profiles
FOR INSERT TO authenticated WITH CHECK (id = auth.uid());

INSERT INTO public.profiles (id, username, display_name)
SELECT id, 'user_' || replace(id::text, '-', ''),
COALESCE(NULLIF(raw_user_meta_data->>'display_name', ''), split_part(email, '@', 1), 'User')
FROM auth.users ON CONFLICT (id) DO NOTHING;

-- Repair creator memberships for conversations left behind by a failed second request.
INSERT INTO public.conversation_members (conversation_id, user_id, role)
SELECT id, created_by, 'OWNER' FROM public.conversations WHERE created_by IS NOT NULL
ON CONFLICT (conversation_id, user_id) DO NOTHING;

INSERT INTO public.conversation_members (conversation_id, user_id, role)
SELECT c.id, p.id, 'MEMBER' FROM public.conversations c CROSS JOIN public.profiles p
WHERE c.type = 'GROUP' AND c.is_private = false AND c.created_by IS NULL
ON CONFLICT (conversation_id, user_id) DO NOTHING;

DROP POLICY IF EXISTS "Members can send messages to their conversations" ON public.messages;
DROP POLICY IF EXISTS "Members can insert messages in their conversations" ON public.messages;
CREATE POLICY "Members can send messages to their conversations" ON public.messages
FOR INSERT TO authenticated
WITH CHECK (sender_id = auth.uid() AND public.is_conversation_member(conversation_id));
DROP POLICY IF EXISTS "Members can view messages in their conversations" ON public.messages;
CREATE POLICY "Members can view messages in their conversations" ON public.messages
FOR SELECT TO authenticated USING (public.is_conversation_member(conversation_id));
COMMIT;
