package greedyhisto;

import java.util.*;

/**
 * Utility class for generating human-friendly display bins from raw GreedyBinner output.
 * <p>
 * Converts irregular adaptive bins into approximately equal-width display bins
 * using 1-2-5 scaling and proportional count redistribution.
 */
public class DisplayBinsUtil {

    /**
     * Converts source bins into approximately {@code targetBins} display bins
     * using "nice" 1-2-5 step intervals and discards low-activity empty bins.
     *
     * @param sourceBins list of raw adaptive bins
     * @param targetBins target number of display bins
     * @param minStep minimum display step size
     * @return list of simplified and human-readable display bins
     */
    public static List<BinEntry> to125DisplayBins(List<BinEntry> sourceBins, int targetBins, int minStep) {
        if (sourceBins == null || sourceBins.isEmpty()) return Collections.emptyList();

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (BinEntry b : sourceBins) {
            if (b.getLower() < min) min = b.getLower();
            if (b.getUpper() > max) max = b.getUpper();
        }
        if (min > max) return Collections.emptyList();

        int span = Math.max(1, max - min + 1);
        int roughStep = Math.max(1, span / Math.max(1, targetBins));
        int step = Math.max(minStep, nice125Step(roughStep));

        if (step > 1) step = step / 2;

        // Snap edges to step grid
        int start = floorToStep(min, step);
        int endExclusive = ceilToStep(max + 1, step);
        int binCount = Math.max(1, (endExclusive - start) / step);

        List<BinEntry> display = new ArrayList<>(binCount);
        for (int i = 0; i < binCount; i++) {
            int lo = start + i * step;
            int hi = start + (i + 1) * step;
            BinEntry be = new BinEntry(lo, hi);
            be.setCount(0);
            display.add(be);
        }

        // Distribute source bin counts across overlapping display bins
        for (BinEntry s : sourceBins) {
            int sLo = s.getLower(), sHi = s.getUpper();
            int sWidth = Math.max(1, sHi - sLo + 1);
            int firstIdx = Math.max(0, (sLo - start) / step);
            int lastIdx = Math.min(binCount - 1, (sHi - start) / step);

            double allocated = 0.0;

            for (int i = firstIdx; i <= lastIdx; i++) {
                BinEntry d = display.get(i);
                int dLo = d.getLower(), dHi = d.getUpper();
                int overlap = overlapLenInclusive(sLo, sHi, dLo, dHi);
                if (overlap <= 0) continue;

                double share = (overlap / (double) sWidth) * s.getCount();
                allocated += share;
                d.setCount(d.getCount() + (int) Math.floor(share));
            }

            // Fix rounding error: adjust bin covering the midpoint
            int mid = sLo + (sWidth - 1) / 2;
            int midIdx = Math.max(0, Math.min(binCount - 1, (mid - start) / step));
            int diff = s.getCount() - (int) Math.round(allocated);
            if (diff != 0) {
                display.get(midIdx).setCount(display.get(midIdx).getCount() + diff);
            }
        }

        // Drop low-count display bins to improve interpretability
        List<BinEntry> filtered = new ArrayList<>();
        for (BinEntry b : display) {
            if (b.getCount() > 10) {
                filtered.add(b);
            }
        }
        return filtered;
    }

    // ---------- internal helpers ----------

    private static int nice125Step(int rough) {
        double exp = Math.floor(Math.log10(rough));
        double base = Math.pow(10, exp);
        int[] mant = {1, 2, 5, 10};
        int best = (int) base;
        for (int m : mant) {
            int s = (int) (m * base);
            if (s >= rough) { best = s; break; }
            best = s;
        }
        return Math.max(1, best);
    }

    private static int floorToStep(int v, int step) {
        int r = v % step;
        return r >= 0 ? v - r : v - (r + step);
    }

    private static int ceilToStep(int v, int step) {
        int r = v % step;
        return r == 0 ? v : (r > 0 ? v + (step - r) : v - r);
    }

    private static int overlapLenInclusive(int aLo, int aHi, int bLo, int bHi) {
        int lo = Math.max(aLo, bLo);
        int hi = Math.min(aHi, bHi);
        return Math.max(0, hi - lo + 1);
    }
}
