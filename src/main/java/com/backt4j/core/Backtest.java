package com.backt4j.core;

import com.backt4j.strategy.Strategy;
import com.backt4j.data.DataPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/***
 * <p>The {@code Backtest} class is the most important class of the {@code backt4j} library. Here the data and the strategy are
 * combined and a Result is produced by backtesting the strategy with the given data.</p>
 * 
 * <p>It is possible to handle different instances of an asset at once (so e.g. multiple stocks at once), however make sure,
 * that there is always an equal amount of price data for every asset available.</p>
 * <p>Also keep in mind that there is no guarantee about the order the prices are fed into the strategy at one point in time. 
 * The prices will be fed into the strategy in order of the time they occured, but it can't be ensured, 
 * that if in one round {@code n}, the price of {@code stock A} was processed after the price of {@code stock B}, that this will 
 * also be the case in round {@code n+1}.</p>
 */
public class Backtest {

    private List<Connection> backtestConnections; 
    public Result results;

    public class Builder {

        private Strategy strategy;
        private List<Exchange> exchanges;
        private List<Connection> customConnections;

        public Builder() {}

        public void add(Strategy aStrategy) throws Exception {
            if (strategy == null) {
                strategy = aStrategy;
            } else {
                throw new Exception("You tried to add two Strategies. " +
                "Please use custom Connections for testing multiple Strategies in one Backtest.");
            }
        }

        public void add(Exchange exchange) {
            if (exchanges == null) {
                List<Exchange> tmpExchanges= new ArrayList<>();
                exchanges = tmpExchanges;
            }
            exchanges.add(exchange);
        }

        public void add(List<Exchange> exchangesList) {
            if (exchanges == null) {
                exchanges = exchangesList;
            } else {
                exchangesList.addAll(exchangesList);
            }
        }

        public void addConnections(List<Connection> connections) throws Exception {
            if (exchanges == null && strategy == null) {
                customConnections = connections;
            } else {
                throw new Exception("You already specified Exchanges or a Strategy. " +
                "custom Connections will overwrite them. Please decide for either custom Connections " +
                "or the standard build method.");
            }
        }

        public Backtest build() throws Exception {
            if (customConnections == null && exchanges == null && strategy == null) {
                throw new Exception("Please add Exchanges and a Strategy Object or a custom Connection.");
            } else if (customConnections != null) {
                return new Backtest(customConnections);
            } else {
                return new Backtest(strategy, exchanges);
            }
        }

    }

    private Backtest(Strategy tradingStrategy, List<Exchange> tradingExchanges) {
        Connection connection = new Connection(tradingExchanges, tradingStrategy);
        backtestConnections.add(connection);
    }

    private Backtest(List<Connection> customConnections) {
        backtestConnections = customConnections;
    }

    /***
     * <p>The {@code run} method iterates through the {@code data} and supplies each {@code Strategy} of a {@code Connection} with the new {@code DataPoint}.</p>
     * <p>Beyond that it updates the {@code currentPrices} classvariable and saves for each asset the latest {@code DataPoint}.</p>
     * <p>This method will also run the Strategy on the Exchanges data and print out all the results in an overview on the Screen.</p>
     * 
     * @throws Exception
     */
    public void run() throws Exception {
        for (Connection connection : backtestConnections) {
            Boolean noData = false;
            while (!noData) {
                for (Exchange exchange : connection.getExchanges()) {
                    HashMap<String, DataPoint> nextDataPoint = exchange.next();
                    for (String key : nextDataPoint.keySet()) {
                        if (nextDataPoint.get(key) == null) {
                                noData = true;
                                break;
                        } else {
                            connection.getStrategy().handleNewPrice(nextDataPoint.get(key));
                        }
                    }
                    if (noData) {
                        break;
                    }
                }
            }

            // Do all necessary operations that must be finalized before the can be show.
            handleRunEnd(connection);

            List<Exchange> exchangesList = connection.getExchanges();
            for (Exchange connectionExchange : exchangesList) {
                results = connectionExchange.getResults();

                System.out.println("\n");
                System.out.println("Results of the Backtest");
                System.out.println("-----------------------");
                System.out.println("\n");
                System.out.println("Relative Performance: " + results.getRelPerformance());
                System.out.println("Absolute Performance: " + results.getAbsPerformance());
                System.out.println("Max Drawdown: " + results.getMaxDrawdown());
                System.out.println("Volatility: " + results.getVolatility());
                System.out.println("\n");
            }

        }
    };

    private void handleRunEnd(Connection connection) {
        for (Exchange exchange : connection.getExchanges()) {
            Double vol = calculateVolatility(exchange.getPerformanceSeries());
            results.setVolatility(vol);
        }
    }

    static double calculateVolatility(List<Double> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("List must not be empty");
        }

        double mean = calculateMean(values);
        double variance = 0.0;

        for (double value : values) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= values.size();

        return Math.sqrt(variance);
    }

    static double calculateMean(List<Double> values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

}
