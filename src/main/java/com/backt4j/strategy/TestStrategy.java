package com.backt4j.strategy;

import java.util.ArrayList;
import java.util.List;

import com.backt4j.core.Connection;
import com.backt4j.core.StockExchange;
import com.backt4j.data.DataPoint;
import com.backt4j.data.PriceDataPoint;

public class TestStrategy implements Strategy {

    final Double buyThreshold;
    final Double performanceThreshold;
    final List<Connection> connections;

    public TestStrategy(Double buy, Double sell, Connection connection) {
        buyThreshold = buy;
        performanceThreshold = sell;
        connections = new ArrayList<>();
        connections.add(connection);
    }

    public TestStrategy(Double buy, Double sell, StockExchange stockExchange) {
        buyThreshold = buy;
        performanceThreshold = sell;
        connections = new ArrayList<>();
        connections.add(new Connection(stockExchange, this));
    }

    public TestStrategy(Double buy, Double sell) {
        buyThreshold = buy;
        performanceThreshold = sell;
        connections = new ArrayList<>();
    }

    @Override
    public void handleNewPrice(DataPoint dataPoint) {
        PriceDataPoint priceDataPoint = (PriceDataPoint) dataPoint;
        Double price = priceDataPoint.open();
        StockExchange stockExchange = (StockExchange) connections.get(0).getExchanges().get(0);
        if (price < buyThreshold) {
            int success = stockExchange.marketOrder(priceDataPoint.id(), 
                                    1000.0, 
                                    priceDataPoint.open(), 
                                    priceDataPoint.window_start().getTime());
            if (success == 1) {
                System.out.println("marketOrder failed with: " + " " + priceDataPoint.id() + " " +
                                    1000.0 + " " +
                                    priceDataPoint.open() + " " +
                                    priceDataPoint.window_start().getTime());
            }
        }

        List<String> openPositionkeys = new ArrayList<>(stockExchange.getOpenPositions().keySet());
        for (String key : openPositionkeys) {
            Double buyIn = stockExchange.getOpenPositions().get(key).get(1);
            if ((price - buyIn)/buyIn >= performanceThreshold) {
                stockExchange.marketClearPosition(key);
            } else if ((price - buyIn)/buyIn < 0) {
                stockExchange.marketClearPosition(key);
            }
        }
    }

    public List<Connection> getConnections() {
        return connections;
    }
    
    public Double getBuyThreshold() {
        return buyThreshold;
    }

    public Double getPerformanceThreshold() {
        return performanceThreshold;
    }

    @Override
    public void addConnection(Connection newConnection) {
        connections.add(newConnection);
    }

}
