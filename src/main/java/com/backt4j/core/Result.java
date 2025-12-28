package com.backt4j.core;

import java.util.ArrayList;
import java.util.List;

/***
 * <p>
 * The Results Object stores performance and risk related indicators that should later be displayed
 * via the {@link Backtest} class.
 * </p>
 * <p>
 * There is not Sharpe Ratio implemented because it can easily be calculated given all other
 * paramenters and it would require a fixed definition of what the Riskfree rate is which is as of
 * today 2025-10-02 not clearly given by the US Treasuries.
 * </p>
 * <p>
 * All the indicators should be recorded by the {@link Exchange} class.
 * </p>
 */
public class Result {

    private double absPerformance;
    private double relPerformance;
    private double volatility;
    private double maxDrawdown;
    private List<Double> performanceSeries;

    /***
     * If values are not specified initially they can be set by the setter methods. At the same time
     * missing values will still be displayed in a reasonable way.
     */
    public Result() {
        performanceSeries = new ArrayList<>();
    }

    public Result(Double absPerformanceDouble, double relPerformanceDouble, double volatilityDouble,
            double biggestLossDouble, List<Double> series) {
        absPerformance = absPerformanceDouble;
        relPerformance = absPerformanceDouble;
        volatility = volatilityDouble;
        maxDrawdown = biggestLossDouble;
        performanceSeries = series;
    }

    public static Result merge(Result first, Result second) {

        Result outputResult = new Result();
        outputResult.setAbsPerformance(first.getAbsPerformance() + second.getAbsPerformance());

        List<Double> aggregateSeries = new ArrayList<>();
        List<Double> firstSeries = new ArrayList<>();
        List<Double> secondSeries = new ArrayList<>();
        for (int i = 0; i < (first.getPerformanceSeries().size() - 1); i++) {
            aggregateSeries.add(i, firstSeries.get(i) + secondSeries.get(i));
        }
        outputResult.setVolatility(Backtest.calculateVolatility(aggregateSeries));

        outputResult.setRelPerformance(
                // Divide the sum of both absolute performances
                (first.getAbsPerformance() + second.getAbsPerformance()) /
                // By the sum of both bases, retrieved via the quotient of absolute Performance and
                // the relative performance.
                        ((first.getAbsPerformance() / first.getRelPerformance())
                                + (second.getAbsPerformance() / second.getRelPerformance())));

        outputResult.setMaxDrawdown(Math.max(first.getMaxDrawdown(), second.getMaxDrawdown()));
        outputResult.setPerformanceSeries(aggregateSeries);

        return outputResult;
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

    void setVolatility(Double volatility) {
        this.volatility = volatility;
    }

    public Double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(Double biggestLoss) {
        this.maxDrawdown = biggestLoss;
    }

    public List<Double> getPerformanceSeries() {
        return performanceSeries;
    }

    public void setPerformanceSeries(List<Double> series) {
        performanceSeries = series;
    }

}
