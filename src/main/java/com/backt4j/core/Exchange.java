package com.backt4j.core;

import com.backt4j.data.Data;
import com.backt4j.data.DataPoint;
import com.backt4j.strategy.Strategy;

import java.util.HashMap;
import java.util.List;

public abstract class Exchange {

    public Data data;
    public List<Connection> connections;
    HashMap<String, DataPoint> currentPrices;
    
    private Results results;

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

    void handleRunEnd() {}

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
