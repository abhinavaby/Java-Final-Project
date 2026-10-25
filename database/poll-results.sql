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
