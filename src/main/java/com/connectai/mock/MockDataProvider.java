package com.connectai.mock;

import com.connectai.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory provider retaining only the three standard channels with access to everyone.
 */
public class MockDataProvider {
    private static final MockDataProvider instance = new MockDataProvider();

    private final User currentUser;
    private final Profile currentProfile;
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private final Map<String, Conversation> conversations = new LinkedHashMap<>();
    private final Map<String, List<Message>> messagesByConversation = new ConcurrentHashMap<>();
    private final Map<String, Poll> polls = new ConcurrentHashMap<>();

    private MockDataProvider() {
        // 1. Current User Profile
        String currentUserId = "00000000-0000-0000-0000-000000000000";
        this.currentUser = new User(currentUserId, "user@connectai.io", "mock-access-token", "mock-refresh-token");
        this.currentProfile = new Profile(currentUserId, "connectai_user", "ConnectAI User", null);
        this.currentProfile.setAbout("Welcome to ConnectAI! Connected and collaborating.");
        this.currentProfile.setOnline(true);
        this.currentProfile.setLastSeen(Instant.now().toString());

        profiles.put(currentUserId, currentProfile);
        Profile sara = new Profile("mock-user-sara", "sara.chen", "Sara Chen", null);
        sara.setOnline(true);
        Profile alex = new Profile("mock-user-alex", "alex.park", "Alex Park", null);
        Profile maya = new Profile("mock-user-maya", "maya.patel", "Maya Patel", null);
        profiles.put(sara.getId(), sara);
        profiles.put(alex.getId(), alex);
        profiles.put(maya.getId(), maya);

        // 2. Initialize the three public channels accessible to everyone
        initGeneralLounge();
        initTechTeamGroupChat();
        initDesignGuildGroupChat();
    }

    public static MockDataProvider getInstance() {
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public List<ConversationMember> getUserMemberships() {
        List<ConversationMember> members = new ArrayList<>();
        for (Conversation conv : conversations.values()) {
            ConversationMember cm = new ConversationMember();
            cm.setConversationId(conv.getId());
            cm.setUserId(currentUser.getId());
            cm.setProfile(currentProfile);
            cm.setConversation(conv);
            cm.setRole(Role.MEMBER);
            members.add(cm);
        }
        return members;
    }

    public Conversation getConversation(String id) {
        return conversations.get(id);
    }

    public List<Profile> searchProfiles(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        return profiles.values().stream()
                .filter(profile -> !profile.getId().equals(currentUser.getId()))
                .filter(profile -> profile.getUsername() != null && profile.getUsername().toLowerCase().contains(normalized))
                .toList();
    }

    public synchronized Conversation startDirectConversation(String partnerUserId) {
        Profile partner = profiles.get(partnerUserId);
        if (partner == null) throw new IllegalArgumentException("User not found.");
        for (Conversation existing : conversations.values()) {
            if (existing.getType() == ConversationType.DIRECT && existing.getDirectPartnerProfile() != null
                    && partnerUserId.equals(existing.getDirectPartnerProfile().getId())) return existing;
        }
        Conversation direct = new Conversation();
        direct.setId("mock-direct-" + UUID.randomUUID().toString().substring(0, 8));
        direct.setType(ConversationType.DIRECT);
        direct.setDirectPartnerProfile(partner);
        direct.setCreatedBy(currentUser.getId());
        direct.setCreatedAt(Instant.now().toString());
        direct.setMembers(List.of(
                new ConversationMember(direct.getId(), currentUser.getId(), Role.MEMBER, currentProfile),
                new ConversationMember(direct.getId(), partner.getId(), Role.MEMBER, partner)));
        conversations.put(direct.getId(), direct);
        messagesByConversation.put(direct.getId(), new ArrayList<>());
        return direct;
    }

    public List<Message> getMessages(String conversationId) {
        return messagesByConversation.getOrDefault(conversationId, new ArrayList<>());
    }

    public synchronized Message sendMessage(String conversationId, String content, String replyToId) {
        Message msg = new Message();
        msg.setId(UUID.randomUUID().toString());
        msg.setConversationId(conversationId);
        msg.setSenderId(currentUser.getId());
        msg.setSenderProfile(currentProfile);
        msg.setMessageType(MessageType.TEXT);
        msg.setContent(content);
        msg.setReplyToMessageId(replyToId);
        msg.setStatus(MessageStatus.SENT);
        msg.setCreatedAt(Instant.now().toString());

        if (replyToId != null) {
            List<Message> list = messagesByConversation.get(conversationId);
            if (list != null) {
                list.stream().filter(m -> m.getId().equals(replyToId)).findFirst().ifPresent(msg::setReplyMessage);
            }
        }

        List<Message> list = messagesByConversation.computeIfAbsent(conversationId, k -> new ArrayList<>());
        list.add(0, msg);

        Conversation conv = conversations.get(conversationId);
        if (conv != null) {
            conv.setLatestMessage(msg);
            conv.setUpdatedAt(msg.getCreatedAt());
        }

        return msg;
    }

    public synchronized Message editMessage(String messageId, String content) {
        for (List<Message> conversationMessages : messagesByConversation.values()) {
            for (Message message : conversationMessages) {
                if (messageId.equals(message.getId()) && currentUser.getId().equals(message.getSenderId())) {
                    message.setContent(content);
                    message.setEditedAt(Instant.now().toString());
                    return message;
                }
            }
        }
        throw new IllegalStateException("Message could not be edited.");
    }

    public synchronized void deleteMessage(String messageId) {
        for (List<Message> conversationMessages : messagesByConversation.values()) {
            for (Message message : conversationMessages) {
                if (messageId.equals(message.getId()) && currentUser.getId().equals(message.getSenderId())) {
                    message.setDeletedAt(Instant.now().toString());
                    message.setContent("This message was deleted.");
                    return;
                }
            }
        }
        throw new IllegalStateException("Message could not be deleted.");
    }

    public synchronized Poll votePoll(String pollId, String optionId, boolean allowsMultiple) {
        Poll poll = polls.get(pollId);
        if (poll == null) return null;

        String userId = currentUser.getId();

        for (PollOption opt : poll.getOptions()) {
            if (opt.getId().equals(optionId)) {
                if (opt.getUserVotes() != null && opt.getUserVotes().contains(userId)) {
                    // A vote remains selected until the user explicitly changes it.
                    if (allowsMultiple) {
                        opt.getUserVotes().remove(userId);
                        opt.setVoteCount(Math.max(0, opt.getVoteCount() - 1));
                    }
                } else {
                    if (opt.getUserVotes() == null) opt.setUserVotes(new ArrayList<>());
                    opt.getUserVotes().add(userId);
                    opt.setVoteCount(opt.getVoteCount() + 1);
                }
            } else if (!allowsMultiple) {
                if (opt.getUserVotes() != null && opt.getUserVotes().contains(userId)) {
                    opt.getUserVotes().remove(userId);
                    opt.setVoteCount(Math.max(0, opt.getVoteCount() - 1));
                }
            }
        }

        int total = 0;
        int voters = 0;
        Set<String> uniqueVoters = new HashSet<>();
        for (PollOption opt : poll.getOptions()) {
            total += opt.getVoteCount();
            if (opt.getUserVotes() != null) uniqueVoters.addAll(opt.getUserVotes());
            opt.setHasVoted(opt.getUserVotes() != null && opt.getUserVotes().contains(userId));
        }
        poll.setTotalVotes(total);
        voters = uniqueVoters.size();
        poll.setTotalVoters(voters);
        poll.setUserVoted(poll.getOptions().stream().anyMatch(PollOption::isHasVoted));
        for (PollOption opt : poll.getOptions()) opt.setPercentage(total == 0 ? 0 : opt.getVoteCount() * 100.0 / total);
        return poll;
    }

    public Poll getPoll(String pollId) { return polls.get(pollId); }

    public synchronized void toggleReaction(String messageId, String emoji) {
        for (List<Message> conversationMessages : messagesByConversation.values()) {
            for (Message message : conversationMessages) {
                if (!messageId.equals(message.getId())) continue;
                List<MessageReaction> reactions = message.getReactions();
                boolean exists = reactions.removeIf(r -> currentUser.getId().equals(r.getUserId()) && emoji.equals(r.getEmoji()));
                if (!exists) reactions.add(new MessageReaction(messageId, currentUser.getId(), emoji));
                return;
            }
        }
    }

    public synchronized Message createPoll(String conversationId, String question, List<String> optionTexts, boolean allowsMultiple, boolean isAnonymous) {
        String pollId = UUID.randomUUID().toString();
        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setConversationId(conversationId);
        poll.setCreatedBy(currentUser.getId());
        poll.setQuestion(question);
        poll.setAllowsMultipleAnswers(allowsMultiple);
        poll.setAnonymous(isAnonymous);
        poll.setCreatedAt(Instant.now().toString());
        poll.setTotalVotes(0);

        List<PollOption> opts = new ArrayList<>();
        int pos = 0;
        for (String txt : optionTexts) {
            PollOption opt = new PollOption();
            opt.setId(UUID.randomUUID().toString());
            opt.setPollId(pollId);
            opt.setOptionText(txt);
            opt.setPosition(pos++);
            opt.setVoteCount(0);
            opt.setUserVotes(new ArrayList<>());
            opts.add(opt);
        }
        poll.setOptions(opts);
        polls.put(pollId, poll);

        Message pollMsg = new Message();
        pollMsg.setId(UUID.randomUUID().toString());
        pollMsg.setConversationId(conversationId);
        pollMsg.setSenderId(currentUser.getId());
        pollMsg.setSenderProfile(currentProfile);
        pollMsg.setMessageType(MessageType.POLL);
        pollMsg.setContent("📊 Poll: " + question);
        pollMsg.setPoll(poll);
        pollMsg.setStatus(MessageStatus.SENT);
        pollMsg.setCreatedAt(Instant.now().toString());

        messagesByConversation.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(0, pollMsg);

        Conversation conv = conversations.get(conversationId);
        if (conv != null) {
            conv.setLatestMessage(pollMsg);
            conv.setUpdatedAt(pollMsg.getCreatedAt());
        }

        return pollMsg;
    }

    public synchronized Conversation createGroup(String name, String description, boolean isPrivate) {
        String convId = "conv-" + UUID.randomUUID().toString().substring(0, 8);
        Conversation c = new Conversation();
        c.setId(convId);
        c.setName(name);
        c.setDescription(description);
        c.setType(ConversationType.GROUP);
        c.setPrivate(isPrivate);
        c.setInviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        c.setCreatedAt(Instant.now().toString());

        List<ConversationMember> members = new ArrayList<>();
        members.add(new ConversationMember(convId, currentUser.getId(), Role.OWNER, currentProfile));
        c.setMembers(members);

        conversations.put(convId, c);
        return c;
    }

    public synchronized Conversation joinGroupByInviteCode(String code) {
        for (Conversation c : conversations.values()) {
            if (c.getType() == ConversationType.GROUP && c.getInviteCode() != null && c.getInviteCode().equalsIgnoreCase(code.trim())) {
                return c;
            }
        }
        return createGroup("Joined Group (" + code.toUpperCase() + ")", "Joined via invite code " + code, false);
    }

    public List<ConversationMember> getConversationMembers(String conversationId) {
        Conversation c = conversations.get(conversationId);
        if (c != null && c.getMembers() != null) {
            return c.getMembers();
        }

        List<ConversationMember> members = new ArrayList<>();
        for (Profile p : profiles.values()) {
            ConversationMember m = new ConversationMember();
            m.setConversationId(conversationId);
            m.setUserId(p.getId());
            m.setProfile(p);
            m.setRole(p.getId().equals(currentUser.getId()) ? Role.OWNER : Role.MEMBER);
            members.add(m);
        }
        return members;
    }

    public AISummaryResponse summarize(String conversationId) {
        Conversation c = conversations.get(conversationId);
        String name = c != null ? c.getDisplayTitle() : "Conversation";

        AISummaryResponse res = new AISummaryResponse();
        res.setSummary("Executive Summary for " + name + ":\n\n" +
                "1. Channel Overview:\n" +
                "   • Realtime communication connected via Supabase backend.\n" +
                "   • All authenticated users have direct open access to this channel.\n\n" +
                "2. Collaboration Status:\n" +
                "   • Instant non-blocking messaging and file sharing are enabled.\n" +
                "   • Team polls and AI rephrasing utilities ready for production.");
        res.setBulletPoints(List.of(
                "Connected to live Supabase backend",
                "Channel accessible to all registered team members",
                "Realtime WebSockets and PostgREST sync enabled"
        ));
        return res;
    }

    public AIRephraseResponse rephrase(String text, Tone tone) {
        AIRephraseResponse res = new AIRephraseResponse();
        res.setOriginalText(text);

        if (tone == Tone.CASUAL) {
            res.setRephrasedText("Hey! " + text + " Let me know what you think!");
        } else if (tone == Tone.CONCISE) {
            res.setRephrasedText(text.replaceAll("(?i)would you mind if|i was thinking that|as per our discussion", "").trim());
        } else if (tone == Tone.ENTHUSIASTIC) {
            res.setRephrasedText("🚀 " + text + " Really excited about this update!");
        } else if (tone == Tone.POLITE) {
            res.setRephrasedText("Kindly note: " + text + ". Thank you for your support!");
        } else {
            res.setRephrasedText("Regarding your inquiry: " + text + ". Please review at your earliest convenience.");
        }

        res.setAlternatives(List.of(
                res.getRephrasedText(),
                "Alternative Option 1: " + text,
                "Alternative Option 2: " + text + " (Optimized)"
        ));
        return res;
    }

    // --- The Three Public Default Channels Accessible to Everyone ---

    private void initGeneralLounge() {
        String id = "conv-general";
        Conversation c = new Conversation();
        c.setId(id);
        c.setName("General Lounge");
        c.setDescription("Public discussion lounge for all team members and newcomers.");
        c.setType(ConversationType.GROUP);
        c.setPrivate(false);
        c.setInviteCode("GENERAL");
        c.setPinned(true);
        c.setCreatedAt(Instant.now().toString());

        List<ConversationMember> members = new ArrayList<>();
        members.add(new ConversationMember(id, currentUser.getId(), Role.OWNER, currentProfile));
        c.setMembers(members);
        conversations.put(id, c);

        List<Message> list = new ArrayList<>();
        Message m1 = createMsg(id, currentProfile, "Welcome to ConnectAI! 👋 This is the General Lounge open to all team members.", "2026-10-12T06:00:00Z");
        list.add(m1);

        messagesByConversation.put(id, list);
        c.setLatestMessage(m1);
    }

    private void initTechTeamGroupChat() {
        String id = "conv-tech-team";
        Conversation c = new Conversation();
        c.setId(id);
        c.setName("Tech & AI Product Team");
        c.setDescription("Core architecture, Java Swing UI components & Supabase Edge Functions");
        c.setType(ConversationType.GROUP);
        c.setPrivate(false);
        c.setInviteCode("TECH2026");
        c.setPinned(false);
        c.setCreatedAt(Instant.now().toString());

        List<ConversationMember> members = new ArrayList<>();
        members.add(new ConversationMember(id, currentUser.getId(), Role.MEMBER, currentProfile));
        c.setMembers(members);
        conversations.put(id, c);

        List<Message> list = new ArrayList<>();
        Message m1 = createMsg(id, currentProfile, "Tech & AI Product Team channel is live. Post technical architecture, code reviews, and updates here.", "2026-10-12T06:05:00Z");
        list.add(m1);

        messagesByConversation.put(id, list);
        c.setLatestMessage(m1);
    }

    private void initDesignGuildGroupChat() {
        String id = "conv-design-guild";
        Conversation c = new Conversation();
        c.setId(id);
        c.setName("Design System Guild 🎨");
        c.setDescription("Swing UI, FlatLaf custom tokens, glassmorphism & desktop UX guidelines");
        c.setType(ConversationType.GROUP);
        c.setPrivate(false);
        c.setInviteCode("DESIGN2026");
        c.setCreatedAt(Instant.now().toString());

        List<ConversationMember> members = new ArrayList<>();
        members.add(new ConversationMember(id, currentUser.getId(), Role.MEMBER, currentProfile));
        c.setMembers(members);
        conversations.put(id, c);

        List<Message> list = new ArrayList<>();
        Message m1 = createMsg(id, currentProfile, "Welcome to the Design System Guild! All design specifications and UI guidelines are tracked here.", "2026-10-12T06:10:00Z");
        list.add(m1);

        messagesByConversation.put(id, list);
        c.setLatestMessage(m1);
    }

    private Message createMsg(String convId, Profile sender, String content, String timeIso) {
        Message m = new Message();
        m.setId(UUID.randomUUID().toString());
        m.setConversationId(convId);
        m.setSenderId(sender.getId());
        m.setSenderProfile(sender);
        m.setMessageType(MessageType.TEXT);
        m.setContent(content);
        m.setStatus(MessageStatus.SENT);
        m.setCreatedAt(timeIso);
        return m;
    }
}
