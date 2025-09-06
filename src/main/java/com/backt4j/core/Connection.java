package com.backt4j.core;

import java.util.List;

import com.backt4j.strategy.Strategy;

/***
 * Connects instances of {@link Strategy} and {@link Exchange}.
 */
public class Connection {

    private List<Exchange> exchanges;
    private List<Strategy> strategies;

    public Connection(List<Exchange> exchangesList, List<Strategy> strategiesList) {
        exchanges = exchangesList;
        strategies = strategiesList;
    }

    public List<Exchange> getExchanges() {
        return exchanges;
    }

    public void setExchanges(List<Exchange> exchanges) {
        this.exchanges = exchanges;
    }

    public List<Strategy> getStrategies() {
        return strategies;
    }

    public void setStrategies(List<Strategy> strategies) {
        this.strategies = strategies;
    }
    
}
