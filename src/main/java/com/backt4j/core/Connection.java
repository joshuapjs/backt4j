package com.backt4j.core;

import java.util.ArrayList;
import java.util.List;

import com.backt4j.strategy.Strategy;

/***
 * Connects instances of {@link Exchange} with a {@link Strategy}.
 */
public class Connection {

    private List<Exchange> exchanges;
    private Strategy strategy;

    public Connection(Exchange exchange, Strategy aStrategy) {
        List<Exchange> tmpExchanges = new ArrayList<>();
        exchanges.add(exchange);
        exchanges = tmpExchanges;

        strategy = aStrategy;
    }

    public Connection(List<Exchange> exchangesList, Strategy aStrategy) {
        exchanges = exchangesList;
        strategy = aStrategy;
    }

    public void add(Exchange exchange) {
        exchanges.add(exchange);
    }

    public void pop() {
        exchanges.remove(exchanges.size() - 1);
    }

    public List<Exchange> getExchanges() {
        return exchanges;
    }

    public void setExchanges(List<Exchange> exchanges) {
        this.exchanges = exchanges;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy aStrategy) {
        this.strategy = aStrategy;
    }
    
}
