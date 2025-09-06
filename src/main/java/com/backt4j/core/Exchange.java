package com.backt4j.core;

import com.backt4j.data.Data;
import com.backt4j.data.DataPoint;
import com.backt4j.strategy.Strategy;

import java.util.HashMap;
import java.util.List;

/***
 * <p>The {@code Exchange} interacts tightly with the {@code Strategy} Object in the following way:</p>
 * <p>On the one hand it supplies the {@code Strategy} instance with data by calling the {@code handleNewPrice} method of the {@code Strategy}.
 * For that it needs to hold a {@code Data} Object. The {@code Data} Object is then unpacked into its {@code DataPoint} Objects during the {@code run} Method call.
 * It is encouraged to implement personalized {@code DataPoint} Objects.</p>
 * <p>On the other hand an {@code Exchange} records the trades initiated by the Strategy class and the performance of the Strategy. 
 * It is therefore naturally expected that an Exchange also calculates the correct values for each class variable of the {@link Results} Object.
 * The Results Object will be requested by the {@link Backtest} class after the {@code Strategy} ran on the data.</p>
 */
public abstract class Exchange {

    /***
     * The Exchange holds a class variable implementing the {@link Data} interface, ensuring that {@code run} will work correctly.
     */
    public Data data;
    /***
     * Every Exchange can have a multitude of {@link Connection} instances.
     * Not every Connection will receive the new {@code DataPoint} at the same time
     */
    public List<Connection> connections;
    /***
     * Exchange stores the most recent {@code DataPoint} it received.
     */
    HashMap<String, DataPoint> currentPrices;
    
    /***
     * This classvariable will be requested by the {@link Backtest} library when its {@code run} method is called.
     */
    private Results results;

    /***
     * <p>The {@code run} method iterates through the {@code data} and supplies each {@code Strategy} of a {@code Connection} with the new {@code DataPoint}.</p>
     * <p>Beyond that it updates the {@code currentPrices} classvariable and saves for each asset the latest {@code DataPoint}.</p>
     * 
     * @throws Exception
     */
    public void run() throws Exception {
        HashMap<String, List<DataPoint>> dataValues = data.getValues();
        for (int i=0; i<dataValues.size(); i++) {
            for (String key : data.getValues().keySet()) {
                for (Connection connection : connections) {
                    for (Strategy strategy : connection.getStrategies()) {
                        currentPrices.put(key, dataValues.get(key).get(i));
                        strategy.handleNewPrice(dataValues.get(key).get(i));
                    }
                }
            }
        }
        handleRunEnd();
    }

    /***
     * <p>This method is always called when the {@code run} method of the {@code Exchange} class terminates.</p>
     * <p>It is intended to do calculations that require all data to be processed by the {@code Strategy} within this method, 
     * e.g. volatility, in order to supply the {@code results} class variable, which is needed for {@code run} method of the {@link Backtest} class, 
     * with the correct values after the Exchange has iterated through all the available data.</p>
     */
    abstract void handleRunEnd();

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

}
