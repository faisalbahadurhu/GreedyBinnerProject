package greedyhisto;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
/**
 * GreedyBinner implements a greedy adaptive binning algorithm for streaming data.
 * 
 * Features:
 * <ul>
 *   <li>Maintains dynamically adjusted bins that capture data distribution compactly.</li>
 *   <li>Supports floating-point and integer ingestion with minimal overhead.</li>
 *   <li>Enforces width and count limits adaptively to control resource usage.</li>
 *   <li>Provides approximate quantile estimation from aggregated bins.</li>
 * </ul>
 * 
 * This class forms the core of the GreedyBinner quantile estimation and visualization
 * system described in the IEEE Access paper.
 */
public class GreedyBinner {
    private final List<BinEntry> bins = new ArrayList<>();
    private static final int MAX_WIDTH = 10;   // number of integer slots allowed per bin (tuneable)
    private int maxBins = 60;                  // maximum number of bins maintained (tuneable)

    // Tunable merge parameters
    private static final int MERGE_GAP_TOLERANCE = 2;  // small gap allowed in relaxed merge pass
    private static final int WIDTH_TOLERANCE = 2;      // small width tolerance in relaxed merge pass

    /**
     * Ingests a single numeric value into the histogram.
     * <p>
     * If the value fits into an existing bin (containment), that bin’s count is incremented.
     * If it is adjacent to a bin, the bin may be extended (respecting {@code MAX_WIDTH}).
     * Otherwise, a new bin is created. After insertion, bin count limits are enforced.
     *
     * @param value the incoming data point (integer or double)
     */
    public synchronized void ingest(double value) {
        boolean isInt = Math.floor(value) == value;
        int ceil = (int) Math.ceil(value);
        int floor = (int) Math.floor(value);

        // 1) Containment: increment bin if it already covers the value
        for (BinEntry bin : bins) {
            if (bin.contains(value)) {
                bin.increment();
                return;
            }
        }

        // 2) Adjacency: extend nearest bin if close enough and width limit allows
        for (BinEntry bin : bins) {
            int L = bin.getLower();
            int Uex = bin.getUpperExclusive();

            // Right extension (for non-integer values near the upper edge)
            if (!isInt && ceil == Uex + 1) {
                int newWidth = (Uex + 1) - L;
                if (newWidth <= MAX_WIDTH) {
                    bin.extendRightOne();
                    bin.increment();
                    return;
                }
            }

            // Left extension (for integers or near-left non-integers)
            if ((isInt && floor == L - 1) || (!isInt && ceil == L)) {
                int newWidth = Uex - (L - 1);
                if (newWidth <= MAX_WIDTH) {
                    bin.extendLeftOne();
                    bin.increment();
                    return;
                }
            }
        }

        // 3) No containment or adjacency → create new single-width bin
        int start = isInt ? (int) value : (int) Math.floor(value);
        BinEntry newBin = new BinEntry(start, start + 1); // default count = 1
        bins.add(newBin);

        // Keep bins sorted by lower bound
        bins.sort(Comparator.comparingInt(BinEntry::getLower));

        // 4) Enforce bin capacity constraints if exceeded
        enforceAdaptiveBinControl();
    }

    /**
     * Enforces {@code maxBins} limit by merging bins adaptively.
     * <p>
     * Merging occurs in multiple passes:
     * <ol>
     *   <li>Strict merge: adjacent bins within {@code MAX_WIDTH}.</li>
     *   <li>Relaxed merge: small gaps and tolerances allowed.</li>
     *   <li>Forced merge: merge least significant adjacent bins as last resort.</li>
     * </ol>
     */
    private void enforceAdaptiveBinControl() {
        if (bins.size() <= maxBins) return;

        while (bins.size() > maxBins) {
            int bestIdx = -1;
            int bestCountSum = Integer.MAX_VALUE;

            // PASS A: strict merges (width <= MAX_WIDTH)
            for (int i = 0; i < bins.size() - 1; i++) {
                BinEntry b1 = bins.get(i);
                BinEntry b2 = bins.get(i + 1);
                int mergedWidth = b2.getUpperExclusive() - b1.getLower();
                if (mergedWidth <= MAX_WIDTH) {
                    int countSum = b1.getCount() + b2.getCount();
                    if (countSum < bestCountSum) {
                        bestCountSum = countSum;
                        bestIdx = i;
                    }
                }
            }
            if (bestIdx != -1) {
                mergeBins(bestIdx, bestIdx + 1);
                continue;
            }

            // PASS B: relaxed merges (allow small gap and width tolerance)
            bestIdx = -1;
            bestCountSum = Integer.MAX_VALUE;
            for (int i = 0; i < bins.size() - 1; i++) {
                BinEntry b1 = bins.get(i);
                BinEntry b2 = bins.get(i + 1);
                int gap = b2.getLower() - b1.getUpperExclusive();
                int mergedWidth = b2.getUpperExclusive() - b1.getLower();
                if (gap <= MERGE_GAP_TOLERANCE && mergedWidth <= (MAX_WIDTH + WIDTH_TOLERANCE)) {
                    int countSum = b1.getCount() + b2.getCount();
                    if (countSum < bestCountSum) {
                        bestCountSum = countSum;
                        bestIdx = i;
                    }
                }
            }
            if (bestIdx != -1) {
                mergeBins(bestIdx, bestIdx + 1);
                continue;
            }

            // PASS C: fallback — merge smallest-count neighbors (ignore width)
            bestIdx = -1;
            bestCountSum = Integer.MAX_VALUE;
            for (int i = 0; i < bins.size() - 1; i++) {
                BinEntry b1 = bins.get(i);
                BinEntry b2 = bins.get(i + 1);
                int countSum = b1.getCount() + b2.getCount();
                if (countSum < bestCountSum) {
                    bestCountSum = countSum;
                    bestIdx = i;
                }
            }
            if (bestIdx != -1) {
                mergeBins(bestIdx, bestIdx + 1);
                continue;
            }

            // No valid merge candidates (safety break)
            break;
        }
    }

    /** 
     * Merges two adjacent bins into a single combined entry.
     * 
     * @param idx1 index of the first bin
     * @param idx2 index of the second bin
     */
    private void mergeBins(int idx1, int idx2) {
        BinEntry b1 = bins.get(idx1);
        BinEntry b2 = bins.get(idx2);

        int newLower = Math.min(b1.getLower(), b2.getLower());
        int newUpperEx = Math.max(b1.getUpperExclusive(), b2.getUpperExclusive());
        int newCount = b1.getCount() + b2.getCount();
        long newLastUpdated = Math.max(b1.getLastUpdated(), b2.getLastUpdated());

        BinEntry merged = new BinEntry(newLower, newUpperEx, newCount, newLastUpdated);
        bins.set(idx1, merged);
        bins.remove(idx2);
    }

    /**
     * Estimates a quantile (e.g., median or 90th percentile) based on cumulative bin counts.
     *
     * @param quantile the target quantile in [0, 1]
     * @return approximate quantile value
     */
    public double estimateQuantile(double quantile) {
        synchronized (bins) {
            int total = bins.stream().mapToInt(BinEntry::getCount).sum();
            if (total == 0) return 0.0;

            int targetRank = (int) Math.ceil(quantile * total);
            int cumulative = 0;

            for (BinEntry bin : bins) {
                cumulative += bin.getCount();
                if (cumulative >= targetRank) {
                    double ratio = (targetRank - (cumulative - bin.getCount())) / (double) bin.getCount();
                    return bin.getLower() + ratio * bin.getWidth();
                }
            }
            return bins.isEmpty() ? 0.0 : bins.get(bins.size() - 1).getPrintedUpper();
        }
    }

    /** Returns a defensive copy of the current bin list. */
    public synchronized List<BinEntry> getBins() {
        return new ArrayList<>(bins);
    }

    /** Adjusts the maximum bin capacity at runtime. */
    public synchronized void setMaxBins(int maxBins) {
        this.maxBins = maxBins;
    }
}
