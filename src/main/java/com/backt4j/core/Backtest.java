package com.backt4j.core;

import com.backt4j.strategy.Strategy;
import com.backt4j.data.DataPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/***
 * <p>The {@code Backtest} class links {@link Strategy} and {@link Exchange} by producing a {@link Result} by backtesting the {@link Strategy} 
 * on the given {@link Exchange}.</p>
 * 
 * <p>It is possible to handle different instances of an asset at once so e.g. multiple stocks, however make sure,
 * that there is always an equal amount of price data points for every asset available.</p>
 * <p>Also keep in mind that there is no guarantee about the order the prices are fed into the strategy at one point in time. 
 * The prices will be fed into the strategy in order of the time they occured, but it can't be ensured, 
 * that if in one round {@code n}, the price of {@code stock A} was processed after the price of {@code stock B}, that this will 
 * also be the case in round {@code n+1}.</p>
 */
public class Backtest {

    /***
     * The Backtest either receives a {@link List} of custom {@link Connection} or preduces its own {@link Connection} by 
     * combining an {@link Exchange} and a {@link Strategy} into one. Custom connections can offer more complex combination 
     * and are as of today 2025-10-03 an experimental feature.
     */
    private List<Connection> backtestConnections; 
    /***
     * an aggregated {@link Result} element, combined of results instances from different {@link Exchange} instances.
     */
    public Result results;

    /***
     * {@code Backtest} follows the Builder Pattern to allow for a more readable and at the same time flexible construction of
     * instances.
     */
    public static class Builder {

        private Strategy strategy;
        private List<Exchange> exchanges;
        private List<Connection> customConnections;

        public Builder() {}

        public Builder add(Strategy aStrategy) throws Exception {
            if (strategy == null) {
                strategy = aStrategy;
            } else {
                throw new Exception("You tried to add two Strategies. " +
                "Please use custom Connections for testing multiple Strategies in one Backtest.");
            }
            return this;
        }

        public Builder add(Exchange exchange) {
            if (exchanges == null) {
                List<Exchange> tmpExchanges= new ArrayList<>();
                exchanges = tmpExchanges;
            }
            exchanges.add(exchange);
            return this;
        }

        public Builder jadd(List<Exchange> exchangesList) {
            if (exchanges == null) {
                exchanges = exchangesList;
            } else {
                exchangesList.addAll(exchangesList);
            }
            return this;
        }

        public Builder addConnections(List<Connection> connections) throws Exception {
            if (exchanges == null && strategy == null) {
                customConnections = connections;
            } else {
                throw new Exception("You already specified Exchanges or a Strategy. " +
                "custom Connections will overwrite them. Please decide for either custom Connections " +
                "or the standard build method.");
            }
            return this;
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
        backtestConnections = new ArrayList<>();
        backtestConnections.add(connection);
        tradingStrategy.addConnection(connection);
    }

    private Backtest(List<Connection> customConnections) {
        backtestConnections = new ArrayList<>();
        backtestConnections = customConnections;
    }

    /***
     * <p>After construction of {@link Strategy}, {@link Exchange} and {@link Backtest}, this method must be called to run the actual backtest.</p>
     * <p>The {@code run} method iterates through the List of {@code Connection} instances and supplies each {@code Strategy} of a 
     * {@code Connection} with all new {@code DataPoint} instances handed over by an {@link Exchange}. If no {@code Connection} was given 
     * during construction of the {@link Exchange}, the {@link Exchange} will generate one by itself. Please have a look at the {@link Backtest.Builder} 
     * for further insights. 
     * It is supported to run the {@link Strategy} with multiple assets at the same time (further information at {@link Exchange}).
     * Beyond that, it is possible to use multiple {@link Exchanges} from multiple connections to allow for very flexible setups and scenarios.</p>
     * <p>The most recent DataPoints are saved in {@code currentPrices} (for each of the multiple assets if applicable).</p>
     * <p>This method will also run the Strategy on the {@link Exchange} data and print out all the results in an overview on the Screen.</p>
     * 
     * @throws Exception because next() throws an exception in case data is data is  {@code null}.
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

            // Do all necessary operations that must be finalized before they can be shown.
            handleRunEnd(connection);

            List<Exchange> exchangesList = connection.getExchanges();
            for (Exchange connectionExchange : exchangesList) {
                results = connectionExchange.getResult();

                System.out.println("\n");
                System.out.println("Results of the Backtest");
                System.out.println("-----------------------");
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
            exchange.getResult().setVolatility(vol);
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
