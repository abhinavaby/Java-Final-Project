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


COMMIT;
