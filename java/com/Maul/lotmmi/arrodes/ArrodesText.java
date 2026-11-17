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
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = cur;
            cur = swap;
        }
        return prev[b.length()];
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
                    if (token.equals(word) || (word.length() >= 7 && fuzzyEquals(token, word))) return true;
                }
            }
        }
        return false;
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
