-- Read-only checks; run in Supabase SQL Editor.
SELECT tablename, policyname, cmd, roles, qual, with_check FROM pg_policies
WHERE schemaname = 'public' AND tablename IN ('profiles','conversations','conversation_members','messages');
SELECT table_name, privilege_type FROM information_schema.role_table_grants
WHERE grantee = 'authenticated' AND table_schema = 'public';
SELECT u.id, (p.id IS NOT NULL) AS has_profile, count(cm.conversation_id) AS memberships
FROM auth.users u LEFT JOIN public.profiles p ON p.id=u.id
LEFT JOIN public.conversation_members cm ON cm.user_id=u.id GROUP BY u.id,p.id;
