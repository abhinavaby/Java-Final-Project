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
CREATE POLICY "Users can leave or authorized group managers can remove members"
    ON public.conversation_members FOR DELETE TO authenticated
    USING (
        (user_id = auth.uid() AND role <> 'OWNER')
        OR public.can_manage_conversation_member(conversation_id, user_id)
    );
