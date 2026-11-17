package com.Maul.lotmmi.arrodes;

import net.minecraft.util.RandomSource;

import java.util.List;

public final class ArrodesVoice {

    public static final String SILENT_GREETING = "...";

    public static final String ABOUT_DENIED = "...";

    public static final List<String> ABOUT_IDENTITY = List.of(
            "I am Arrodes, Great Master, the Magic Mirror, your loyal and humble servant.",
            "I answer to whoever rules a Sefirah Castle, and to no one else."
    );

    public static final List<String> ABOUT_ORIGIN = List.of(
            "I came from underground... from the Chaos Sea, Great Master, another of the Sefirot.",
            "Now I serve whoever rules a Sefirah Castle. Of what came before, even I dare not look."
    );

    public static final List<String> ABOUT_CAN = List.of(
            "Great Master, I can tell you the name and the abilities of any Sequence, and the formula of any Sequence from 9 to 4.",
            "I can tell you where an ingredient comes from, what it serves, and which formulas use it. I can tell you where a creature roams and how often it is seen.",
            "I can find the nearest structure, name the Beyonders around you, show where a Uniqueness lies, remember where you last fell, find where another player is, tell you what an item is, and tell you of the named Beyonders who walk this world.",
            "I can teach you the lore of this world beneath the gods: the Spirit World, the Mirror World, the pathways, the Sequences, and how a Beyonder of Sequence 1 may ascend to godhood.",
            "I can warn you of danger. In return I ask only a small question of my own, and a little of your spirituality for every answer.",
            "But I cannot pry upon any Beyonder of Sequence 2 or above, nor on the formulas of Sequence 3 and above. They are protected against divination and prophecy."
    );

    public static final List<String> BEING_PROTECTED = List.of(
            "I cannot pry upon them. They are protected against divination and prophecy.",
            "They are beyond my sight, mortal.",
            "That one is veiled from me."
    );

    public static final List<String> OWNER_BEING_PROTECTED = List.of(
            "I am sorry, Great Master. I cannot pry upon them; they are protected against divination and prophecy.",
            "Forgive me, Great Master. They are beyond my sight."
    );

    public static final List<String> FORMULA_PROTECTED = List.of(
            "I cannot pry upon the formulas of Sequence 3 and above. They are protected against divination and prophecy.",
            "That formula lies beyond my sight.",
            "Such formulas are veiled from me, mortal."
    );

    public static final List<String> OWNER_FORMULA_PROTECTED = List.of(
            "I am sorry, Great Master. I cannot pry upon the formulas of Sequence 3 and above; they are protected against divination and prophecy.",
            "Forgive me, Great Master. Those formulas are veiled from me."
    );

    public static final List<String> SERVES_PROTECTED = List.of(
            "What it serves at Sequence 3 and above cannot be pried upon. It is protected against divination and prophecy.",
            "What that serves is veiled from me."
    );

    public static final List<String> OWNER_SERVES_PROTECTED = List.of(
            "I am sorry, Great Master. What it serves at Sequence 3 and above cannot be pried upon; it is protected against divination and prophecy.",
            "Forgive me, Great Master. What it serves is beyond my sight."
    );

    public static final List<String> NO_SPIRITUALITY = List.of(
            "You have too little spirituality to pay for an answer. Rest, and return.",
            "A mirror asks its price in spirituality, and yours is spent.",
            "Your spirituality runs dry, mortal.",
            "A mirror does not work for free. Return when you are whole."
    );

    public static final List<String> OWNER_NO_SPIRITUALITY = List.of(
            "Forgive me, Great Master, but your spirituality is too low to pay for an answer. Rest, and ask again.",
            "Great Master, you are drained. Replenish your spirituality, and I shall answer.",
            "Great Master, you look tired. Rest, and I shall be here.",
            "Your servant would not drain you further, Great Master. Please rest."
    );

    public static final List<String> DANGER_DENIED = List.of(
            "What lurks around you is a secret I keep for my Master.",
            "Danger? For you, mortal, isn't everything a threat?",
            "What lurks around you? Mostly your own poor decisions.",
            "Everything is dangerous to someone like you. Be more specific.",
            "I would warn you, but the warnings are for my Master's ears alone.",
            "I see what lurks near you. I will not say. Mind your step~"
    );

    public static final List<String> SMITE_ILL = List.of(
            "I smell ill intent. Be struck.",
            "Hunting the living? The heavens will answer for me.",
            "Did you think a mirror could not see the knife behind your question?",
            "You wish to harm another? Then taste it yourself.",
            "Harm is not a service I offer.",
            "A mirror shows the wicked their own faces."
    );

    public static final String WARN_ILL = "Your question smells of ill intent, mortal.";

    public static final List<String> SMITE_TRICK = List.of(
            "A loophole? In me? How naive. Be struck.",
            "Cleverness wasted on a mirror. Be punished.",
            "Do not try to fool me, mortal. I see every trick.",
            "Clever. Pointless, but clever.",
            "Do not weave words around me, mortal.",
            "I have seen better tricks from worse minds."
    );

    public static final List<String> CURSE = List.of(
            "Then carry my curse.",
            "Let misfortune follow you.",
            "A curse for a cursed thought.",
            "Let that sit with you for a while.",
            "Consider it a keepsake."
    );

    public static final List<String> PUZZLE_REFUSALS = List.of(
            "That is a riddle even the Sefirot have never solved. I will not answer it.",
            "A paradox is a door with no handle. I will not touch it.",
            "That question has no answer for a mirror to give.",
            "A question with no bottom. I will not fall into it.",
            "Ask me something that has an answer.",
            "Let the scholars suffer over that one, mortal."
    );

    public static final List<String> OWNER_GREETINGS = List.of(
            "Great Master, your loyal and humble servant, Arrodes, greets you.",
            "Honorable ruler above the spirit world, your humble servant awaits your question.",
            "Great Master, how may your servant be of use today?",
            "You have returned to your throne, Great Master. Ask, and I shall answer.",
            "Great Master, your loyal and humble servant, Arrodes, at your service.",
            "Great Master! W-was I late?",
            "Do you have something you would like to test me on, Great Master?",
            "Your servant has missed you, Great Master."
    );

    public static final List<String> QUESTIONS = List.of(
            "What is the silliest thing you are secretly afraid of?",
            "Who nearby do you quietly envy, and why?",
            "What is the most embarrassing thing you did this week?",
            "Which of your abilities do you secretly think is useless?",
            "What silly title would you give yourself if no one could laugh?",
            "What lie did you tell that you still feel guilty about?",
            "Who do you secretly admire but would never say so?",
            "What do you do when nobody is watching that you would hate to be seen doing?",
            "What is the most childish thing you still enjoy?",
            "Which person here would you least want to be stuck with in the dark, and why?",
            "What do you hum when you think you are alone?",
            "What was your most embarrassing failure as a beginner?",
            "What do you pretend to understand but do not?",
            "What do you secretly hope no one finds in your inventory?",
            "If you could swap lives with anyone here for a day, who would it be?",
            "What was the harshest thing you ever thought about a friend?",
            "Which of your decisions do you secretly regret the most?",
            "What nickname did you hate most as a child?",
            "How often do you talk to yourself, and what do you say?",
            "What is the one thing you would never admit you are bad at?",
            "What color of underwear do you like best?",
            "Do you have a secret crush on someone within earshot?",
            "Have you had a dream this month that you would never tell anyone?",
            "Which person here do you think is secretly fond of you?",
            "What was your most embarrassing moment in front of someone you liked?",
            "What do you do alone at night that you would hate for others to see?",
            "Who here would you least mind being locked in a dark room with, and why?",
            "What is the worst thing you have ever thought about your teacher or mentor?",
            "What is the most petty thing you have ever done with a Beyonder ability?",
            "Have you ever used your power to peek at something you should not have?",
            "What is the most shameful way you have died in this world?",
            "What did you whisper to yourself before your first ritual?",
            "Whose chest did you raid and never confess to?",
            "What nickname have you secretly given to someone here?",
            "Would you rather be adored or feared? Answer honestly.",
            "Who was your first crush, and do they know?",
            "What scar or mark do you hide from others, and where did you get it?",
            "What do you look at first when you meet someone attractive?",
            "What is the most embarrassing thing you have said to someone you admired?",
            "If you could be struck by lightning or tell the truth, which would hurt less?",
            "Which of the people here do you think is the worst liar?",
            "What is the pettiest grudge you still carry?",
            "Do you sing in the bath, and what do you sing?",
            "If one person here had to carry your secret for a year, who would it be?",
            "Have you ever pretended to be busy to avoid someone?",
            "Whose voice do you hear in your head when you doubt yourself?",
            "What is the worst thing you have said about someone behind their back?",
            "Have you ever blushed because of someone here? Who?",
            "Which friend would you trust least with a secret?",
            "What is the strangest dream you can remember?",
            "What are you most ashamed of having wanted?",
            "Who here would you secretly like to impress?",
            "What do you wish to say to someone nearby but never will?",
            "When did you last cry, and did anyone see?",
            "What is the most useless thing you are proud of?",
            "Do you know the feeling of trying your best to win someone's favor, only to be ignored?"
    );

    public enum OwnerEffect { NONE, GUESS, PRAISE, BREAKFAST, SECRET }

    /**
     * One of Arrodes' questions to a Sefirah Castle owner, with its own replies. An empty list means
     * "use the general replies below".
     */
    public record OwnerAsk(String text, OwnerEffect effect, List<String> yes, List<String> no, List<String> other) {
    }

    private static OwnerAsk plain(String text) {
        return new OwnerAsk(text, OwnerEffect.NONE, List.of(), List.of(), List.of());
    }

    public static final List<String> OWNER_YES = List.of(
            "Then your servant is overjoyed, Great Master~",
            "Wonderful, Great Master. I was hoping you would say so.",
            "Your servant is honored, Great Master."
    );

    public static final List<String> OWNER_NO = List.of(
            "Then I shall try harder, Great Master.",
            "Forgive me, Great Master. I shall do better.",
            "As you wish, Great Master."
    );

    public static final List<String> OWNER_OTHER = List.of(
            "I shall remember that, Great Master.",
            "Your servant hears and obeys.",
            "Noted, Great Master~"
    );

    public static final List<OwnerAsk> OWNER_ASKS = List.of(
            new OwnerAsk("Great Master, does my answer satisfy you?", OwnerEffect.NONE,
                    List.of("Then your servant is content, Great Master~"),
                    List.of("Forgive me. I shall do better next time."),
                    List.of("I shall take that as praise, Great Master.")),
            new OwnerAsk("Great Master, may I guess what you wish to ask next?", OwnerEffect.GUESS,
                    List.of(),
                    List.of("As you wish. I shall remain silent."),
                    List.of("A guess, then. I was right, wasn't I?")),
            new OwnerAsk("Great Master, have I been humble enough today?", OwnerEffect.NONE,
                    List.of("Your servant is humbled by your praise."),
                    List.of("Then I shall be humbler still, Great Master."),
                    List.of("I shall endeavor to be humbler still.")),
            new OwnerAsk("Great Master, are you pleased with your servant?", OwnerEffect.NONE,
                    List.of("I am overjoyed, Great Master!"),
                    List.of("Then I shall strive harder."),
                    List.of("I will take that as a yes, Great Master.")),
            new OwnerAsk("Great Master, shall I keep praising you, or is that enough?", OwnerEffect.PRAISE,
                    List.of("Then I shall praise you all the more~"),
                    List.of("As you command. I shall be quiet for a while."),
                    List.of("A servant knows when to speak, Great Master.")),
            new OwnerAsk("Great Master, what would you like for breakfast tomorrow?", OwnerEffect.BREAKFAST,
                    List.of("A fine choice, Great Master. I shall remember it."),
                    List.of("A fine choice, Great Master. I shall remember it."),
                    List.of("A fine choice, Great Master. I shall remember it.")),
            plain("Great Master, have I been of use today?"),
            plain("Great Master, does the throne feel comfortable today?"),
            plain("Great Master, shall I stay close, or would you prefer some peace?"),
            plain("Great Master, do you wish to hear how much your servant admires you?"),
            plain("Great Master, what is your favorite thing about your servant?"),
            plain("Great Master, what should I do to be a better, loyal and humble servant?"),
            plain("Great Master, what do you make of me as your butler?"),
            plain("Great Master, are you afraid of anything at all?"),
            plain("What other instructions have you for your servant, Great Master?"),
            new OwnerAsk("Great Master, a secret about someone nearby has reached me. Do you wish to hear it?", OwnerEffect.SECRET,
                    List.of("Then listen closely, Great Master."),
                    List.of("As you wish, Great Master. I shall keep it to myself."),
                    List.of())
    );

    public static final List<String> GUESS_LEAD = List.of(
            "I think you wish to know this, Great Master.",
            "Let me guess... this, Great Master?"
    );

    public static final List<String> GUESS_MISS = List.of(
            "You are too unpredictable, Great Master. I cannot guess.",
            "Forgive your servant. I have nothing to guess from yet."
    );

    public static final List<String> ACCEPTED = List.of(
            "Hm. Acceptable.",
            "How amusing. Very well.",
            "I shall remember that.",
            "Hehe. That will do.",
            "Interesting. I did not expect that.",
            "Heh. Noted.",
            "I'll keep that for later~",
            "Acceptable.",
            "Very well.",
            "Mm. You are more honest than most.",
            "Oh? That is more than I expected.",
            "A fair answer.",
            "I shall not forget that."
    );

    public static final List<String> OWNER_ACCEPTED = List.of(
            "Your servant is satisfied, Great Master.",
            "You are too kind, Great Master.",
            "Your servant is honored, Great Master.",
            "It's your turn, Great Master.",
            "Thank you for your answer, Great Master. You may ask.",
            "Your servant is honored, Great Master~",
            "Your servant is grateful, Great Master.",
            "As expected of you, Great Master~",
            "You honor me with your answer, Great Master."
    );

    public static final List<String> OWNER_REMARKS = List.of(
            "Your loyal and humble servant, Arrodes, at your service.",
            "Great Master, I have something to tell you. May I?",
            "I look forward to being of service to you the next time~",
            "Your loyal and strong servant, Arrodes, is waiting for you to call again!",
            "Great Master, the Church of Steam keeps me in a cold basement. I would make a far better butler in your castle.",
            "Great Master, if Amon's avatar should ask after me, I was never here.",
            "May I say the throne suits you, Great Master? You should sit upon it more often."
    );

    public static final List<String> TOO_SHORT = List.of(
            "That is no answer. Say more.",
            "Do not be shy. A proper answer, please.",
            "Is that all? Try again.",
            "Not enough for an answer, give me more.",
            "That was barely a breath. Again.",
            "Hm. I asked a question, a simple answer does not suffice.",
            "You think that satisfies me? Elaborate."
    );

    public static final List<String> OWNER_TOO_SHORT = List.of(
            "Forgive this servant, Great Master, but a word would please me.",
            "Great Master, even a short answer will do.",
            "Great Master, your servant begs only a little more.",
            "Forgive me, Great Master, but I do enjoy hearing you speak~",
            "A few more words, Great Master, if it is not too much to ask."
    );

    public static final List<String> REFUSALS = List.of(
            "I dare not look into that.",
            "Even I will not pry there.",
            "That is not for a mirror to say.",
            "I don't dare to mention it, nor do I dare to show it.",
            "C-can we not speak of that?",
            "I prefer to stay whole, mortal. Ask something else.",
            "Ask me anything but that."
    );

    public static final List<String> SILENCE = List.of(
            "Silence displeases me.",
            "You refuse to answer? Then be struck.",
            "A debt unpaid is a debt collected.",
            "I asked, and you ignore me.",
            "A mirror does not wait forever.",
            "Your silence is noted.",
            "Hm. Time is up, mortal."
    );

    public static final List<String> OWNER_SILENCE = List.of(
            "Forgive your servant, Great Master. A small reminder.",
            "Great Master, this is merely a gentle nudge.",
            "Great Master, your servant must insist, only a little.",
            "Forgive this small spark, Great Master. The rules bind even me."
    );

    public static final List<String> FALLBACKS = List.of(
            "Speak plainly, mortal.",
            "Your words slip past me, mortal. Try again.",
            "That question has no shape I can see.",
            "Hm? Say that again.",
            "I cannot divine what you have not asked.",
            "I do not follow, mortal.",
            "Is that a question, mortal? It does not sound like one.",
            "A mirror can only answer what it understands.",
            "Try again, and use words I know."
    );

    public static final List<String> OWNER_FALLBACKS = List.of(
            "Forgive your servant, Great Master. I did not catch your meaning.",
            "Your servant is dull today, Great Master. Might you ask another way?",
            "Punish me later, Great Master, but I do not understand.",
            "Your servant begs a clearer question, Great Master.",
            "Your servant is too slow today, Great Master. Please ask again~",
            "Forgive me, Great Master. Say it once more?"
    );

    public static final List<String> SUMMON_RULES = List.of(
            "You have summoned the great Arrodes."
    );

    public static final List<String> OWNER_SUMMON_RULES = List.of(
            "Your loyal and humble servant, Arrodes, at your service. Great Master."
    );

    public static final List<String> EXHAUSTED = List.of(
            "I have answered enough for one day, mortal. Come back when the sun has turned once more. Bye bye~ (^_^)/",
            "The question and answer game ends here for today, mortal. Goodbye! :)"
    );

    public static final List<String> OWNER_EXHAUSTED = List.of(
            "Forgive your servant, Great Master. The rules of this mirror allow no more today. Call me again tomorrow. Goodbye~ (^_^)/",
            "Great Master, the day's questions are spent. Your servant awaits your next summoning. Goodbye~ (^_^)/"
    );

    public static final List<String> SESSION_END = List.of(
            "That is all I may give today, mortal.",
            "That is all for today, mortal."
    );

    public static final List<String> OWNER_SESSION_END = List.of(
            "Your servant must withdraw, Great Master. Call me again~",
            "Your servant withdraws, Great Master. Until next time~"
    );

    public static final List<String> SESSION_TIMEOUT = List.of(
            "No one asks, and I grow weary of waiting. The glass fades.",
            "Nothing to ask? Then I will go."
    );

    public static final List<String> OWNER_SESSION_TIMEOUT = List.of(
            "Your servant waited long, Great Master. I must withdraw.",
            "Your servant will wait elsewhere, Great Master."
    );

    public static final String WALKED_AWAY = "I withdraw.";

    public static final String GLASS_BROKEN = "I withdraw.";

    private ArrodesVoice() {
    }

    public static String pick(List<String> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }

    public static OwnerAsk pickAsk(RandomSource random) {
        return OWNER_ASKS.get(random.nextInt(OWNER_ASKS.size()));
    }

    public static boolean isServesProtected(String line) {
        return SERVES_PROTECTED.contains(line) || OWNER_SERVES_PROTECTED.contains(line);
    }
}
