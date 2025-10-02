package com.backt4j.core;

import com.backt4j.data.Data;
import com.backt4j.data.DataPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/***
 * <p>The {@code Exchange} interacts tightly with the {@code Strategy} Object in the following way:</p>
 * <p>On the one hand it supplies the {@code Strategy} instance with data by calling the {@code handleNewPrice} method of the {@code Strategy}.
 * For that it needs to hold a {@code Data} Object. The {@code Data} Object is then unpacked into its {@code DataPoint} Objects during the {@code run} Method call.
 * It is encouraged to implement personalized {@code DataPoint} Objects.</p>
 * <p>On the other hand an {@code Exchange} records the trades initiated by the Strategy class and the performance of the Strategy. 
 * It is therefore naturally expected that an Exchange also calculates the correct values for each class variable of the {@link Result} Object.
 * The Results Object will be requested by the {@link Backtest} class after the {@code Strategy} ran on the data.</p>
 */
public abstract class Exchange {

    /***
     * The Exchange holds a class variable implementing the {@link Data} interface, ensuring that {@code run} will work correctly.
     */
    public Data data;
    private HashMap<String, Iterator<DataPoint>> dataIterators;
    private List<Double> performanceSeries;

    public Exchange(Data exchangeData) {
        data = exchangeData;
        dataIterators = new HashMap<String, Iterator<DataPoint>>();
        performanceSeries = new ArrayList<Double>();
    }

    /***
     * The Exchange stores the most recent {@code DataPoint} it received.
     */
    HashMap<String, DataPoint> currentPrices;

    public HashMap<String, DataPoint> next() throws Exception {

        if (data == null) {
            throw new Exception("The data classvariable is null. No Data instance was provided.");
        }

        if (dataIterators.isEmpty()) {
            for (String key : data.getValues().keySet()) {
                dataIterators.put(key, data.getValues().get(key).iterator());
            }
        }

        HashMap<String, DataPoint> nextDataPoints = new HashMap<>();
        for (String key : data.getValues().keySet()) {
            if (dataIterators.get(key).hasNext()) {
                nextDataPoints.put(key, dataIterators.get(key).next());
            } else {
                nextDataPoints.put(key, null);
            }
        }

        performanceSeries.add((getCurrentAccountValue() - getAccountValue())/getAccountValue());
        
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

    abstract public Double getAccountValue();

    abstract public Double getCurrentAccountValue();

    abstract public Result getResults();

}
