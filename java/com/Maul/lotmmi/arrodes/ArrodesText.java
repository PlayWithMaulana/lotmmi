package com.Maul.lotmmi.arrodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArrodesText {

    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
            Map.entry("zero", 0), Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3),
            Map.entry("four", 4), Map.entry("five", 5), Map.entry("six", 6), Map.entry("seven", 7),
            Map.entry("eight", 8), Map.entry("nine", 9), Map.entry("ii", 2), Map.entry("iii", 3),
            Map.entry("iv", 4), Map.entry("vi", 6), Map.entry("vii", 7), Map.entry("viii", 8), Map.entry("ix", 9));

    private static final Set<String> STOP = Set.of("the", "of", "and", "a", "an", "to", "in", "on", "for", "my", "me", "is", "are", "this", "that");

    private ArrodesText() {
    }

    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static List<String> tokens(String text) {
        String normalized = normalize(text);
        List<String> out = new ArrayList<>();
        if (normalized.isEmpty()) return out;
        for (String token : normalized.split(" ")) out.add(stripOrdinal(token));
        return out;
    }

    private static String stripOrdinal(String token) {
        if (token.matches("\\d+(st|nd|rd|th)")) return token.replaceAll("[a-z]+$", "");
        return token;
    }

    public static boolean isStop(String token) {
        return STOP.contains(token);
    }

    public static int distance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int best = Math.min(Math.min(d[i][j - 1] + 1, d[i - 1][j] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    best = Math.min(best, d[i - 2][j - 2] + 1);
                }
                d[i][j] = best;
            }
        }
        return d[n][m];
    }

    public static boolean fuzzyEquals(String a, String b) {
        if (a.equals(b)) return true;
        int len = Math.min(a.length(), b.length());
        if (len < 5) return false;
        int allowed = len >= 9 ? 2 : 1;
        return distance(a, b) <= allowed;
    }

    public static int numberOf(String token) {
        if (token.matches("\\d")) return Integer.parseInt(token);
        Integer word = NUMBER_WORDS.get(token);
        return word == null ? -1 : word;
    }

    public static int findSequence(List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("sequence") || token.equals("seq") || token.equals("sequences")) {
                for (int j = i + 1; j < Math.min(tokens.size(), i + 3); j++) {
                    int n = numberOf(tokens.get(j));
                    if (n >= 0) return n;
                }
                for (int j = i - 1; j >= Math.max(0, i - 2); j--) {
                    int n = numberOf(tokens.get(j));
                    if (n >= 0) return n;
                }
            }
        }
        for (String token : tokens) {
            if (token.matches("\\d")) return Integer.parseInt(token);
        }
        for (String token : tokens) {
            if (!token.equals("one") && !token.equals("i")) {
                int n = numberOf(token);
                if (n >= 0) return n;
            }
        }
        return -1;
    }

    public static boolean hasAny(List<String> tokens, String normalizedText, String... words) {
        for (String word : words) {
            if (word.contains(" ")) {
                if ((" " + normalizedText + " ").contains(" " + word + " ")) return true;
            } else {
                for (String token : tokens) {
                    if (token.equals(word)) return true;
                    if (word.length() >= 7 && token.length() >= 6 && fuzzyEquals(token, word) && onlyClose(token, word, words)) return true;
                }
            }
        }
        return false;
    }

    private static boolean onlyClose(String token, String word, String[] words) {
        for (String other : words) {
            if (other.equals(word) || other.contains(" ")) continue;
            if (distance(token, other) <= 1) return false;
        }
        return true;
    }

    /** Exact matches only: used for words that can trigger a punishment, where a typo must never be guessed at. */
    public static boolean hasExact(List<String> tokens, String normalizedText, String... words) {
        for (String word : words) {
            if (word.contains(" ")) {
                if ((" " + normalizedText + " ").contains(" " + word + " ")) return true;
            } else if (tokens.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** True when any of the first words sits within {@code window} tokens of any of the second words. */
    public static boolean near(List<String> tokens, Set<String> first, Set<String> second, int window) {
        for (int i = 0; i < tokens.size(); i++) {
            if (!first.contains(tokens.get(i))) continue;
            int from = Math.max(0, i - window);
            int to = Math.min(tokens.size() - 1, i + window);
            for (int j = from; j <= to; j++) {
                if (j != i && second.contains(tokens.get(j))) return true;
            }
        }
        return false;
    }

    public enum Reply { YES, NO, OTHER, KEEP, ENOUGH }

    private static final Set<String> YES_WORDS = Set.of("yes", "yeah", "yep", "yup", "sure", "indeed", "please", "ok", "okay", "definitely",
            "absolutely", "certainly", "satisfied", "pleased", "happy", "good", "great", "fine", "excellent", "wonderful", "perfect",
            "correct", "right", "gladly", "aye", "affirmative");

    private static final Set<String> NO_WORDS = Set.of("no", "nope", "nah", "never", "not", "dont", "don", "enough", "stop", "wrong",
            "bad", "displeased", "unhappy", "poor", "terrible", "negative");

    private static final Set<String> KEEP_WORDS = Set.of("keep", "continue", "more", "praise", "again", "yes", "yeah", "sure", "please");

    private static final Set<String> ENOUGH_WORDS = Set.of("enough", "stop", "quiet", "silence", "silent", "hush", "no", "nope", "nah");

    /** Sorts an owner's reply to one of Arrodes' questions into yes, no or something else. */
    public static Reply classify(String text) {
        List<String> tokens = tokens(text);
        for (String token : tokens) if (NO_WORDS.contains(token)) return Reply.NO;
        for (String token : tokens) if (YES_WORDS.contains(token)) return Reply.YES;
        return Reply.OTHER;
    }

    /** Sorts an owner's reply to "shall I keep praising you, or is that enough?". */
    public static Reply classifyPraise(String text) {
        List<String> tokens = tokens(text);
        for (String token : tokens) if (ENOUGH_WORDS.contains(token)) return Reply.ENOUGH;
        for (String token : tokens) if (KEEP_WORDS.contains(token)) return Reply.KEEP;
        return Reply.OTHER;
    }

    /** Normalises a book's text for comparison with an honorific name. */
    public static String squash(String text) {
        return normalize(text);
    }

    public static String pretty(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder out = new StringBuilder();
        for (String part : path.split("[_/ ]+")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public static String direction(double dx, double dz) {
        String[] names = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        double degrees = (Math.toDegrees(Math.atan2(dz, dx)) + 360.0D) % 360.0D;
        return names[(int) Math.round(degrees / 45.0D) % 8];
    }
}
