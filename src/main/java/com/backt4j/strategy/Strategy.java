package com.backt4j.strategy;

import java.util.List;

import com.backt4j.data.DataPoint;
import com.backt4j.core.Connection;

public interface Strategy {

    public void handleNewPrice(DataPoint dataPoint);

    public void setConnections(List<Connection> exchange);
    public List<Connection> getConnections();

}
