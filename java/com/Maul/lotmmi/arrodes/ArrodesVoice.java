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
            "I can teach you the lore of this world beneath the gods: the Spirit World, the pathways, the Sequences, and how a Beyonder of Sequence 1 may ascend to godhood.",
            "I can warn you of danger. In return I ask only a small question of my own, and a little of your spirituality for every answer.",
            "But I cannot pry upon any Beyonder of Sequence 2 or above, nor on the formulas of Sequence 3 and above. They are protected against divination and prophecy."
    );

    public static final String BEING_PROTECTED = "I cannot pry upon them. They are protected against divination and prophecy.";

    public static final String OWNER_BEING_PROTECTED = "I am sorry, Great Master. I cannot pry upon them; they are protected against divination and prophecy.";

    public static final String FORMULA_PROTECTED = "I cannot pry upon the formulas of Sequence 3 and above. They are protected against divination and prophecy.";

    public static final String OWNER_FORMULA_PROTECTED = "I am sorry, Great Master. I cannot pry upon the formulas of Sequence 3 and above; they are protected against divination and prophecy.";

    public static final String SERVES_PROTECTED = "What it serves at Sequence 3 and above cannot be pried upon. It is protected against divination and prophecy.";

    public static final String OWNER_SERVES_PROTECTED = "I am sorry, Great Master. What it serves at Sequence 3 and above cannot be pried upon; it is protected against divination and prophecy.";

    public static final List<String> NO_SPIRITUALITY = List.of(
            "You have too little spirituality to pay for an answer. Rest, and return.",
            "A mirror asks its price in spirituality, and yours is spent."
    );

    public static final List<String> OWNER_NO_SPIRITUALITY = List.of(
            "Forgive me, Great Master, but your spirituality is too low to pay for an answer. Rest, and ask again.",
            "Great Master, you are drained. Replenish your spirituality, and I shall answer."
    );

    public static final String DANGER_DENIED = "What lurks around you is a secret I keep for my Master.";

    public static final String LORE_DENIED = "The secrets of this world are kept for my Master alone.";

    public static final List<String> SMITE_ILL = List.of(
            "I smell ill intent. Be struck.",
            "Hunting the living? The heavens will answer for me.",
            "Did you think a mirror could not see the knife behind your question?",
            "You wish to harm another? Then taste it yourself."
    );

    public static final List<String> SMITE_TRICK = List.of(
            "A loophole? In me? How naive. Be struck.",
            "Cleverness wasted on a mirror. Be punished.",
            "Do not try to fool me, mortal. I see every trick."
    );

    public static final List<String> CURSE = List.of(
            "Then carry my curse.",
            "Let misfortune follow you.",
            "A curse for a cursed thought."
    );

    public static final List<String> PUZZLE_REFUSALS = List.of(
            "That is a riddle even the Sefirot have never solved. I will not answer it.",
            "A paradox is a door with no handle. I will not touch it.",
            "That question has no answer for a mirror to give."
    );

    public static final List<String> OWNER_GREETINGS = List.of(
            "Great Master, your loyal and humble servant, Arrodes, greets you.",
            "Honorable ruler above the spirit world, your humble servant awaits your question.",
            "Great Master, how may your servant be of use today?",
            "You have returned to your throne, Great Master. Ask, and I shall answer."
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
            "What is the one thing you would never admit you are bad at?"
    );

    public static final List<String> OWNER_QUESTIONS = List.of(
            "Great Master, does my answer satisfy you?",
            "Great Master, may I guess what you wish to ask next?",
            "Great Master, have I been humble enough today?",
            "Great Master, are you pleased with your servant?",
            "Great Master, shall I keep praising you, or is that enough?",
            "Great Master, what would you like for breakfast tomorrow?"
    );

    public static final List<String> ACCEPTED = List.of(
            "Hm. Acceptable.",
            "How amusing. Very well.",
            "I shall remember that.",
            "Hehe. That will do."
    );

    public static final List<String> OWNER_ACCEPTED = List.of(
            "Your servant is satisfied, Great Master.",
            "You are too kind, Great Master.",
            "Your servant is honored, Great Master."
    );

    public static final List<String> OWNER_TIPS = List.of(
            "Great Master, rest within your castle and no eye in the world will find you.",
            "Great Master, ask me where any ingredient hides and I shall tell you.",
            "Great Master, a Uniqueness is never lost. It only waits for a hand.",
            "Great Master, a potion is mastered by acting its part, not merely by drinking it.",
            "Great Master, lightning is not particular in a storm. Mind where you stand.",
            "Great Master, the strong hide their names. Ask me of the weak and I shall not fail you."
    );

    public static final List<String> TOO_SHORT = List.of(
            "That is no answer. Say more.",
            "Do not be shy. A proper answer, please.",
            "Is that all? Try again."
    );

    public static final List<String> OWNER_TOO_SHORT = List.of(
            "Forgive this servant, Great Master, but a word would please me.",
            "Great Master, even a short answer will do."
    );

    public static final List<String> REFUSALS = List.of(
            "I dare not look into that.",
            "Even I will not pry there.",
            "That is not for a mirror to say."
    );

    public static final List<String> SILENCE = List.of(
            "Silence displeases me.",
            "You refuse to answer? Then be struck.",
            "A debt unpaid is a debt collected."
    );

    public static final List<String> OWNER_SILENCE = List.of(
            "Forgive your servant, Great Master. A small reminder.",
            "Great Master, this is merely a gentle nudge."
    );

    private ArrodesVoice() {
    }

    public static String pick(List<String> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }
}
