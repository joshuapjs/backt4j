import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import com.backt4j.core.Connection;
import com.backt4j.core.StockExchange;
import com.backt4j.data.DataPoint;
import com.backt4j.data.PriceDataPoint;
import com.backt4j.strategy.Strategy;

public class TestStrategy implements Strategy {

    final Double buyThreshold;
    final Double performanceThreshold;
    final List<Connection> connections;
    StockExchange exchange;

    public TestStrategy(Double buy, Double sell, Connection connection) {
        buyThreshold = buy;
        performanceThreshold = sell;
        connections = new ArrayList<>();
        connections.add(connection);
    }

    @Override
    public void handleNewPrice(DataPoint dataPoint) {
        PriceDataPoint priceDataPoint = (PriceDataPoint) dataPoint;
        Double price = priceDataPoint.open();
        if (price < buyThreshold) {
            exchange.marketOrder(priceDataPoint.id(), 
                            Double.valueOf(100.0), 
                            priceDataPoint.open(), 
                            priceDataPoint.window_start().getTime());
        }

        Set<Entry<String,List<Double>>> openPositionEntries = exchange.getOpenPositions().entrySet();
        for (Entry<String,List<Double>> entry : openPositionEntries) {
            Double buyIn = entry.getValue().get(0);
            if ((price - buyIn)/buyIn >= performanceThreshold) {
                exchange.marketClearPosition(entry.getKey());
            }
        }
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void addConnection(Connection newConnection) {
        connections.add(newConnection);
    }
    
    public Double getBuyThreshold() {
        return buyThreshold;
    }

    public StockExchange getExchange() {
        return exchange;
    }

    public Double getPerformanceThreshold() {
        return performanceThreshold;
    }
    
}
