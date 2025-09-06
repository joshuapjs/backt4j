package com.backt4j.core;

import com.backt4j.strategy.Strategy;

/***
 * The Backtest class is the most important class of the backt4j library. Here the data and the strategy are
 * combined and a Result is produced by backtesting the strategy with the given data.
 * 
 * It is possible to handle different instances of an asset at once (so e.g. multiple stocks at once), however make sure,
 * that there is always an equal amount of price data for every asset available. Also keep in mind that there is no guarantee 
 * about the order prices are fed into the model at one point in time. The prices will be fed into the strategy 
 * in order of the time they occured, but it can't be ensured, that if in one round n, the price of stock A 
 * was processed after the price of stock B, that this will also be the case in round n+1.
 */
public class Backtest {

    public Strategy strategy;
    public Results results;
    public Exchange exchange;

    public Backtest(Strategy tradingStrategy, Exchange tradingExchange) {
        strategy = tradingStrategy;
        exchange = tradingExchange;
    }

    /***
     * This method will run the data on the Strategy and print out
     * all the results in an overview on the Screen.
     */
    public void run() throws Exception {

        results = exchange.getResults();

        System.out.println("\n");
        System.out.println("Results of the Backtest");
        System.out.println("-----------------------");
        System.out.println("\n");
        System.out.println("Relative Performance: " + results.relPerformance);
        System.out.println("Absolute Performance: " + results.absPerformance);
        System.out.println("Max Drawdown: " + results.maxDrawdown);
        System.out.println("Volatility: " + results.volatility);
        System.out.println("\n");
    };

}
