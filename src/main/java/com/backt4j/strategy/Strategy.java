package com.backt4j.strategy;

import com.backt4j.data.DataPoint;
import com.backt4j.core.Connection;

/***
 * <p>Every {@code Strategy} that should be backtested must implement this interface.
 * A class implementing this interface can be part of a {@link Connection} and interacts closely with the {@link Strategy } class.</p>
 * <p>All Connections a {@code Strategy} holds, must only have this one {@code Strategy} in the {@code List} of its Strategies.</p>
 */
public interface Strategy {

    public void handleNewPrice(DataPoint dataPoint);
    public void addConnection(Connection connection);

}
