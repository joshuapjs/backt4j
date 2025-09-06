package com.backt4j.strategy;

import java.util.List;

import com.backt4j.data.DataPoint;
import com.backt4j.core.Connection;

/***
 * Every {@code Strategy} that should be backtested must implement this interface.
 * A class implementing this interface can be part of a {@link Connection} and interacts closely with the {@link Strategy } class.
 */
public interface Strategy {

    public void handleNewPrice(DataPoint dataPoint);

    public void setConnections(List<Connection> exchange);
    public List<Connection> getConnections();

}
