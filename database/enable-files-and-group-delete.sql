-- Run in Supabase SQL Editor after the app tables have been created.
BEGIN;
GRANT DELETE ON public.conversations TO authenticated;
DROP POLICY IF EXISTS "Creators can delete their groups" ON public.conversations;
CREATE POLICY "Creators can delete their groups" ON public.conversations
FOR DELETE TO authenticated USING (type = 'GROUP' AND created_by = auth.uid());

GRANT SELECT, INSERT ON public.attachments TO authenticated;
INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES ('chat-attachments', 'chat-attachments', false, 52428800)
ON CONFLICT (id) DO UPDATE SET file_size_limit = EXCLUDED.file_size_limit;

-- Replace the previous broad storage policies with conversation membership checks.
DROP POLICY IF EXISTS "Authenticated users can upload attachments" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can read attachments" ON storage.objects;
DROP POLICY IF EXISTS "Chat members can upload files" ON storage.objects;
DROP POLICY IF EXISTS "Chat members can read files" ON storage.objects;
CREATE POLICY "Chat members can upload files" ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'chat-attachments' AND EXISTS (
    SELECT 1 FROM public.conversation_members cm
    WHERE cm.user_id = auth.uid() AND cm.conversation_id::text = (storage.foldername(name))[1]
));
CREATE POLICY "Chat members can read files" ON storage.objects FOR SELECT TO authenticated
USING (bucket_id = 'chat-attachments' AND EXISTS (
    SELECT 1 FROM public.conversation_members cm
    WHERE cm.user_id = auth.uid() AND cm.conversation_id::text = (storage.foldername(name))[1]
));
COMMIT;
