package com.Maul.lotmmi.arrodes;

import java.util.ArrayList;
import java.util.List;

/** Picks, for a name of several words, the stretch of a question where those words really sit together. */
final class ArrodesSpan {

    private ArrodesSpan() {
    }

    /**
     * {@code candidates.get(i)} lists the positions in the question where word i of the name matches. Returns one
     * position for every word that matched anywhere, chosen so they sit as close together as possible (the earliest
     * group wins a tie). In "the hunter pathway, and the blood of thousand faced hunter" the name claims the second
     * "hunter", and the first one is left for the pathway.
     */
    static List<Integer> tightest(List<List<Integer>> candidates) {
        List<List<Integer>> parts = new ArrayList<>();
        for (List<Integer> positions : candidates) {
            if (!positions.isEmpty()) parts.add(positions);
        }
        List<Integer> out = new ArrayList<>();
        if (parts.isEmpty()) return out;

        int[] best = new int[parts.size()];
        int[] now = new int[parts.size()];
        int[] bestSpread = {Integer.MAX_VALUE};
        search(parts, 0, now, best, bestSpread, Integer.MAX_VALUE, Integer.MIN_VALUE);
        if (bestSpread[0] == Integer.MAX_VALUE) {
            // Two words matched the same single word and could not be told apart: take the first of each.
            for (List<Integer> positions : parts) {
                if (!out.contains(positions.get(0))) out.add(positions.get(0));
            }
            return out;
        }
        for (int position : best) out.add(position);
        return out;
    }

    private static void search(List<List<Integer>> parts, int index, int[] now, int[] best, int[] bestSpread, int low, int high) {
        if (index == parts.size()) {
            int spread = high - low;
            if (spread < bestSpread[0]) {
                bestSpread[0] = spread;
                System.arraycopy(now, 0, best, 0, now.length);
            }
            return;
        }
        for (int position : parts.get(index)) {
            boolean taken = false;
            for (int i = 0; i < index; i++) {
                if (now[i] == position) taken = true;
            }
            if (taken) continue;
            int nextLow = Math.min(low, position);
            int nextHigh = Math.max(high, position);
            if (nextHigh - nextLow >= bestSpread[0]) continue;
            now[index] = position;
            search(parts, index + 1, now, best, bestSpread, nextLow, nextHigh);
        }
    }
}
