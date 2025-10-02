package com.backt4j.core;

import java.util.HashMap;

/***
 * <p>The Results Object stores performance and risk related indicators that should later be displayed via 
 * the {@link Backtest} class.</p>
 * <p>All the indicators should be recorded by the {@link Exchange} class.</p>
 */
public class Results {

    private Double absPerformance;
    private Double relPerformance;
    private Double volatility;
    private Double maxDrawdown;

    /***
     * If values are not specified initially they can be set by the setter methods.
     * At the same time missing values will still be displayed in a reasonable way.
     */
    public Results() {
        relPerformance = null;
        absPerformance = null;
        volatility = null;
        maxDrawdown = null;
    }

    public Results(Double absPerformanceDouble, 
                    Double relPerformanceDouble,
                    Double volatilityDouble, 
                    Double biggestIncreaseDouble,
                    Double biggestLossDouble,
                    HashMap<Double, Double> distributionHashMap) {

                        relPerformance = absPerformanceDouble;
                        absPerformance = absPerformanceDouble;
                        volatility = volatilityDouble;
                        maxDrawdown = biggestLossDouble;

    }

    public Double getAbsPerformance() {
        return absPerformance;
    }

    public void setAbsPerformance(Double absPerformance) {
        this.absPerformance = absPerformance;
    }

    public Double getRelPerformance() {
        return relPerformance;
    }

    public void setRelPerformance(Double relPerformance) {
        this.relPerformance = relPerformance;
    }

    public Double getVolatility() {
        return volatility;
    }

    public void setVolatility(Double volatility) {
        this.volatility = volatility;
    }

    public Double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(Double biggestLoss) {
        this.maxDrawdown = biggestLoss;
    }

}
