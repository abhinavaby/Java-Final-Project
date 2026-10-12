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
