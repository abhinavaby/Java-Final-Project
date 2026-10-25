-- ConnectAI 2.2.0 upgrade: group departure/ownership transfer and profile pictures.
-- Idempotent: run in Supabase Dashboard > SQL Editor on an existing v2 database.
BEGIN;

CREATE OR REPLACE FUNCTION public.leave_group(target_conversation uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    actor uuid := auth.uid();
    actor_role text;
    target_type text;
    successor uuid;
BEGIN
    IF actor IS NULL THEN
        RAISE EXCEPTION 'Sign in before leaving a group' USING ERRCODE = '42501';
    END IF;
    SELECT cm.role, c.type INTO actor_role, target_type
      FROM public.conversation_members cm
      JOIN public.conversations c ON c.id = cm.conversation_id
      WHERE cm.conversation_id = target_conversation AND cm.user_id = actor
      FOR UPDATE OF cm;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'You are not a member of this group';
    END IF;
    IF target_type <> 'GROUP' THEN
        RAISE EXCEPTION 'You can only leave a group conversation';
    END IF;

    IF actor_role = 'OWNER' THEN
        SELECT user_id INTO successor
          FROM public.conversation_members
          WHERE conversation_id = target_conversation AND user_id <> actor
          ORDER BY CASE role WHEN 'ADMIN' THEN 0 ELSE 1 END, joined_at, user_id
          LIMIT 1
          FOR UPDATE;
        IF successor IS NULL THEN
            RAISE EXCEPTION 'Add another member before leaving as the only group owner';
        END IF;
        PERFORM pg_catalog.set_config('connectai.owner_transfer', 'on', true);
        UPDATE public.conversation_members
           SET role = 'MEMBER'
           WHERE conversation_id = target_conversation AND user_id = actor;
        UPDATE public.conversation_members
           SET role = 'OWNER'
           WHERE conversation_id = target_conversation AND user_id = successor;
        UPDATE public.conversations SET created_by = successor WHERE id = target_conversation;
        PERFORM pg_catalog.set_config('connectai.owner_transfer', 'off', true);
    END IF;
    DELETE FROM public.conversation_members
      WHERE conversation_id = target_conversation AND user_id = actor;
END;
$$;
REVOKE ALL ON FUNCTION public.leave_group(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.leave_group(uuid) TO authenticated;

INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES ('avatars', 'avatars', true, 5242880)
ON CONFLICT (id) DO UPDATE SET public = true, file_size_limit = 5242880;

DROP POLICY IF EXISTS "Users can upload their own profile pictures" ON storage.objects;
CREATE POLICY "Users can upload their own profile pictures"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Public profile pictures are readable" ON storage.objects;
CREATE POLICY "Public profile pictures are readable"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'avatars');


-- Resolve private invite codes and add the authenticated caller without exposing
-- private conversations through the normal conversation SELECT policy.
CREATE OR REPLACE FUNCTION public.join_group_by_invite_code(p_invite_code text)
RETURNS SETOF public.conversations
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    target_group public.conversations;
    actor uuid := auth.uid();
BEGIN
    IF actor IS NULL THEN
        RAISE EXCEPTION 'Sign in before joining a group' USING ERRCODE = '42501';
    END IF;
    IF p_invite_code IS NULL OR length(trim(p_invite_code)) < 4 THEN
        RAISE EXCEPTION 'Enter a valid group invite code';
    END IF;

    SELECT c.* INTO target_group
      FROM public.conversations c
      WHERE c.type = 'GROUP'
        AND upper(c.invite_code) = upper(trim(p_invite_code))
      LIMIT 1;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No group found with that invite code';
    END IF;

    INSERT INTO public.conversation_members (conversation_id, user_id, role)
    VALUES (target_group.id, actor, 'MEMBER')
    ON CONFLICT (conversation_id, user_id) DO NOTHING;
    RETURN NEXT target_group;
    RETURN;
END;
$$;
REVOKE ALL ON FUNCTION public.join_group_by_invite_code(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.join_group_by_invite_code(text) TO authenticated;

COMMIT;

-- Only the ownership-transfer RPC sets this transaction-local flag.
CREATE OR REPLACE FUNCTION public.protect_group_member_role()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role
       AND pg_catalog.current_setting('connectai.owner_transfer', true) IS DISTINCT FROM 'on' THEN
        IF OLD.role = 'OWNER' OR NOT public.can_manage_conversation_member(OLD.conversation_id, OLD.user_id) THEN
            RAISE EXCEPTION 'You do not have permission to change this member role' USING ERRCODE = '42501';
        END IF;
        IF NOT public.is_conversation_owner(OLD.conversation_id) AND NEW.role <> 'MEMBER' THEN
            RAISE EXCEPTION 'Admins cannot promote members or assign the owner role' USING ERRCODE = '42501';
        END IF;
    END IF;
    RETURN NEW;
END; $$;
NOTIFY pgrst, 'reload schema';
