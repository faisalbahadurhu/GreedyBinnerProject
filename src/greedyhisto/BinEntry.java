package greedyhisto;

/**
 * Represents a single histogram bin with inclusive lower bound
 * and exclusive upper bound [lower, upperExclusive).
 * Each bin tracks its count and last update time.
 */
public class BinEntry {
    private int lower;
    private int upperExclusive;
    private int count;
    private long lastUpdated;

    /** Constructs a new bin covering [lower, upperExclusive) with initial count = 1. */
    public BinEntry(int lower, int upperExclusive) {
        this(lower, upperExclusive, 1, System.currentTimeMillis());
    }

    /** Full constructor for internal merging operations. */
    public BinEntry(int lower, int upperExclusive, int count, long lastUpdated) {
        this.lower = lower;
        this.upperExclusive = upperExclusive;
        this.count = count;
        this.lastUpdated = lastUpdated;
    }

    /** Checks if a value lies within [lower, upperExclusive). */
    public boolean contains(double value) {
        return value >= lower && value < upperExclusive;
    }

    /** Increments count by one. */
    public void increment() {
        count++;
        this.lastUpdated = System.currentTimeMillis();
    }

    /** Increments count by a specific amount. */
    public void increment(int amount) {
        count += amount;
        this.lastUpdated = System.currentTimeMillis();
    }

    /** Extends bin one unit to the left (decrease lower by 1). */
    public void extendLeftOne() {
        lower--;
        this.lastUpdated = System.currentTimeMillis();
    }

    /** Extends bin one unit to the right (increase upperExclusive by 1). */
    public void extendRightOne() {
        upperExclusive++;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getCount() { return count; }
    public int getLower() { return lower; }
    public int getUpperExclusive() { return upperExclusive; }
    public int getPrintedUpper() { return upperExclusive - 1; }
    public long getLastUpdated() { return lastUpdated; }

    /** Returns the integer width of this bin. */
    public int getWidth() {
        return upperExclusive - lower;
    }

    public int getUpper() {
        return getUpperExclusive();
    }

    /** Returns formatted label like “30–39”. */
    public String getRangeLabel() {
        return String.format("%d-%d", lower, upperExclusive);
    }

    public void setCount(int i) {
        this.count = i;
    }

    @Override
    public String toString() {
        return getRangeLabel() + ":" + count;
    }
}
