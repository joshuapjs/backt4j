import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

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

    @Override
    public void handleNewPrice(DataPoint dataPoint) {
        PriceDataPoint priceDataPoint = (PriceDataPoint) dataPoint;
        Double price = priceDataPoint.open();
        StockExchange stockExchange = (StockExchange) connections.get(0).getExchanges().get(0);
        if (price < buyThreshold) {
            stockExchange.marketOrder(priceDataPoint.id(), 
                            Double.valueOf(100.0), 
                            priceDataPoint.open(), 
                            priceDataPoint.window_start().getTime());
        }

        Set<Entry<String,List<Double>>> openPositionEntries = stockExchange.getOpenPositions().entrySet();
        for (Entry<String,List<Double>> entry : openPositionEntries) {
            Double buyIn = entry.getValue().get(0);
            if ((price - buyIn)/buyIn >= performanceThreshold) {
                stockExchange.marketClearPosition(entry.getKey());
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

    public Double getPerformanceThreshold() {
        return performanceThreshold;
    }
    
}
