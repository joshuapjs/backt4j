package com.backt4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.backt4j.data.Data;
import com.backt4j.data.DataPoint;
import com.backt4j.strategy.Strategy;

/***
 * <h2>How to implement</h2>
 * 
 * <p>The Methods to implement are {@code getInitialBudget}, {@code getCurrentPortfolioValue}, {@code getRemainingBudget} and {@code getResult}. 
 * 
 * <ul>
 * <li>{@code getInitialBudget}: Should return a double reflecting the initial budget given to the {@code Exchange} which serves as a bound to prevent unlimited trading.</li>
 * <li>{@code getCurrentPortfolioValue}: Should return the current value of the portfolio. This should be updated during every call with the help 
 * of {@code currentPrices} and some classvariable that tracks the assets in the portfolio. It is the goal that this returns a double reflecting the 
 * current monetary value of the portfolio.</li>
 * <li>{@code getRemainingBudget}: Should return the remaining fictive monetary budget of the fictive account of the {@link Strategy} at {@code Exchange}. This should be updated 
 * every time a trade was made by the respective monetary costs or returns.</li>
 * </ul>  
 * In order to calculate all measures correctly the {@code initialBudget} should be {@code final} as it's the initially assigned budget before any trade was made. 
 * The {@code getCurrentPortfolioValue} should return the {@code currentAccountValue} which shoud be updated every time it is called with the help of currentPrices and some classvariable 
 * tracking the current portfolio. This means in your own implementation of {@code Exchange} this value must be updated after every transaction and of course the 
 * bound provided by {@code getRemainingBudget} must be checked prior to an transaction so that it never goes below zero. The methods {@code getInitialBudget}, {@code getCurrentPortfolioValue}
 * are used for calculating the performance of the Strategy with every new {@link DataPoint} supplied by {@code next}.
 * The {@code getResult} method provides a {@link Result} Object. The calculation of the volatility is handled by the Framework based on the performance values generated 
 * with every new price supplied via {@code next}.</p>
 * 
 * <h2>Purpose and functionality</h2>
 * 
 * <p>The {@code Exchange} interacts tightly with the {@code Strategy} Object in the following way:</p>
 * 
 * <p>On the one hand it's intended to supply the {@link Strategy} instance with new DataPoints every time the {@code next} method 
 * is called (I say intended because {@code Exchange.next} simply returns a {@link HashMap} with the respecitive {@link DataPoint} 
 * to ANY method calling it). This happens within the {@code Backtest.run} method which calls {@code Exchange.next} to retrieve the {@link DataPoint} and forwards 
 * it to the {@code Strategy.handleNewPrice} method. This is the reason for the {@code Exchange} to hold a {@link Data} Object. 
 * The {@link Data} Object is then unpacked into its {@link DataPoint} Objects during the {@code run} method call, making the process described possible. 
 * It is encouraged to implement personalized {@link DataPoint} classes.</p>
 * 
 * <p>On the other hand an {@code Exchange} records the trades initiated by the Strategy class and the performance of the {@link Strategy}. 
 * It is therefore expected that an {@code Exchange} also calculates the correct values for each classvariable of the {@link Result} Object.
 * The {@link Result} Object will be requested by the {@link Backtest} class after the {@link Strategy} ran on the {@link Data}.</p>
 * 
 * <p>It is possible to handle different instances of an asset at once (so e.g. multiple stocks at once), however make sure,
 * that there is always an equal amount of price data for every asset available. For further information visit {@code README.md}</p>
 * <p>Also there is no guarantee about the order determining how the prices are fed into the strategy. 
 * The prices will be fed into the strategy in order of the time they occured, but it can't be ensured, 
 * that if in one round {@code n}, the price of {@code Stock A} was processed after the price of {@code Stock B}, that this will 
 * also be the case in round {@code n+1}.</p>
 */
public abstract class Exchange {

    /***
     * <p>The Exchange must hold a data classvariable implementing the {@link Data} interface,
     * in order for {@code run} to work properly.</p>
     */
    public Data data;
    /***
     * <p>dataIterator stores the Iterators for each of the {@code List<DataPoint>} elements from {@code Data}.
     * This enables {@code run} to sequentially work through each of the lines of data, one column at a time. 
     * This behavior is desired because if Data contains values for multiple Assets we want to simulate that the {@code Strategy}
     * receives trade resulted price updates for each of the Stocks but a different times. As we iterate later {@code HashMap}
     * through a {@code Set} of keys there is at least theoretically no guaranteed order we get the new {@link DataPoint} from each of
     * the Iterators stored as values in dataIterators.</p>
     */
    private HashMap<String, Iterator<DataPoint>> dataIterators;
    /***
     * <p>This list will be used by {@link Backtest} to compute the volatility the Strategies performance. It is collected automatically
     * using the users implementations of {@code getInitialBudget} and {@code getCurrentPortfolioValue}.</p>
     */
    private List<Double> performanceSeries;

    /***
     * <p>The Exchange stores the most recent {@code DataPoint} it received.</p>
     */
    HashMap<String, DataPoint> currentPrices;

    public Exchange(Data exchangeData) {
        data = exchangeData;
        dataIterators = new HashMap<String, Iterator<DataPoint>>();
        performanceSeries = new ArrayList<Double>();
    }

    /***
     * <p>Returns a {@link HashMap} containing {@link DataPoint} instead of a single {@link DataPoint} to allow that the {@link Strategy} 
     * can run on multiple Assets all at once. Effectively a single line of data is returned.
     * @return {@code HashMap<String, DataPoint>} that represents a single line of data.</p>
     * @throws Exception in case data is {@code null}.
     */
    public HashMap<String, DataPoint> next() throws Exception {

        if (data == null) {
            throw new Exception("The data classvariable is null. No Data instance was provided.");
        }

        // Create all iterators in case next() is called for the first time. is called for the first time.
        if (dataIterators.isEmpty()) {
            for (String key : data.getValues().keySet()) {
                dataIterators.put(key, data.getValues().get(key).iterator());
            }
        }

        // Iterate of the keys of dataIteratos to get all values from one line of data at once.
        HashMap<String, DataPoint> nextDataPoints = new HashMap<>();
        for (String key : data.getValues().keySet()) {
            if (dataIterators.get(key).hasNext()) {
                nextDataPoints.put(key, dataIterators.get(key).next());
            } else {
                nextDataPoints.put(key, null);
            }
        }

        // Calculate the current performance of the portfolio to later get the volatility of the respective Strategy. 
        performanceSeries.add((getCurrentPortfolioValue() - getInitialBudget())/getInitialBudget());
        
        return nextDataPoints;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public List<Double> getPerformanceSeries() {
        return performanceSeries;
    };

    /***
     * <p>Should return the initial budget that was allocated to test the {@link Strategy} with. 
     * It is recommended to set the respective classvariable as {@code final}.</p>
     * @return A double called initialBudget or similar.</p>
     */
    abstract public Double getInitialBudget();

    /***
     * <p>Should return the current portfolio value that resulted from the operations of a {@link Strategy} instance. 
     * In order to update the value effectively the classvariable {@code currentPrices} can be used to update the currentValue.
     * The implementation is left to the user because some user might want to add their remaining Budget, effectively the cash, 
     * to the portfolio value. As one main goal of this Framework is to be not to restrictive, this feature is not provided 
     * here but in finished implementation of {@code Exchange}, e.g. {@link StockExchange}.</p>
     * @return A double called currentPortfolioValue or similar.
     */
    abstract public Double getCurrentPortfolioValue();

    /***
     * <p>Should return the remaining budget to restrict the {@link Strategy} from using more money than it was allowed to use for trading.
     * @return remainingBudget</p>
     */
    abstract public Double getRemainingBudget();

    /***
     * <p>Should return the {@link Result} filled with values for all its parameters accessible via setters.
     * This Method is called after the {@link Strategy} ran on all {@link DataPoints} of {@link Data}.
     * @return Result</p>
     */
    abstract public Result getResult();

}
