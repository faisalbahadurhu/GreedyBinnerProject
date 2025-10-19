# 🧠 GreedyBinner

**GreedyBinner** is an Edge-level(single-machine) adaptive histogram construction framework that balances **accuracy**, **compactness**, and **real-time interpretability**.  
It uses a **greedy binning algorithm** that dynamically merges or splits bins based on data distribution, protecting key quantiles while limiting bin count and width.

---

## 🚀 Features

- ✅ Greedy, adaptive binning for streaming data  
- 📊 Real-time histogram visualization  
- 🎯 Quantile protection and adaptive resolution control  
- 🧮 Supports comparisons with *t-digest* and *DDSketch*  
- ⚙️ Fully implemented in **Java** with a lightweight GUI

---

### 📦 Download

You can download the compiled JAR directly from here:  
👉 [GreedyBinner.jar](https://github.com/faisalbahadurhu/GreedyBinnerProject/raw/main/GreedyBinner.jar)

*(Click the link above to download the executable JAR file.)*

---

### ⚙️ V.Simple Usage Example

```java
// 1. Import the GreedyBinner class
import greedyhisto.GreedyBinner;
import greedyhisto.DisplayBinUtil;
import java.util.List;
public class Example {
    public static void main(String[] args) {
        GreedyBinner binner = new GreedyBinner();

        // 2. Ingest some data
        double[] data = {1, 1, 2, 5, 100, 101, 105};
        for (double d : data) {
            binner.ingest(d);
        }
        // 3. Retrieve bins
        List<BinEntry> bins = binner.getBins();
        //  4. Print labels with count
      for (BinEntry bin : bins) {
        System.out.println(bin.getRangeLabel()+"  "+bin.getCount());
            }
        // Build clean display bins — e.g., aim for ~5, and don’t use step < 10
            List lushBins = DisplayBinsUtil.to125DisplayBins(snapshot, 5, 10);
           //  4. Print lush labels with count
      for (BinEntry bin : lushBins) {
        System.out.println(bin.getRangeLabel()+"  "+bin.getCount());
            }

    }
}
## 📊 TGB vs Prevalent schemes
 
[Relative Error Comparison Comparison](https://github.com/faisalbahadurhu/GreedyBinnerProject/blob/main/image.png)


