import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import org.junit.jupiter.api.Test;
import com.backt4j.core.StockExchange;
import com.backt4j.data.CSVData;

public class StockExchangeTest {

        @Test
        public void basicFunctionalityTest() throws Exception {

                // initialize test
                File resourcesDirectory = new File("src/test/resources");
                CSVData csvData = (CSVData) (new CSVData("csv-data-name"))
                                .init(resourcesDirectory.getAbsolutePath() + "/testdata.csv");
                assert csvData.getValues() != null;
                StockExchange stockExchange = new StockExchange(1000000000, csvData);

                // Test if Market Orders add a Transaction to the OpenPositions Queue
                stockExchange.marketOrder("AAPL", 1000, 100, 1234567);
                assertEquals(stockExchange.getOpenPositions().get("AAPL").getFirst().amount(), 1000);
                assertEquals(stockExchange.getOpenPositions().get("AAPL").getFirst().price(), 100);
                assertEquals(stockExchange.getOpenPositions().get("AAPL").getFirst().timeStamp(), 1234567);

                // Test if the Orders are sorted by arrival by adding another Order with the
                // expectation that it will be new first element.
                stockExchange.marketOrder("AAPL", 1000, 100, 7654321);
                assertEquals(stockExchange.getOpenPositions().get("AAPL").getFirst().timeStamp(), 7654321);

                // Test neutralization of the Orders by selling all Positions. Both Transactions
                // should be removed from the Dequeue after this and the OpenPositionAmount
                // should yield 0.
                stockExchange.marketOrder("AAPL", -2000, 10000, 1234567);
                assertEquals(stockExchange.getOpenPositionAmounts().get("AAPL"), 0);
                assertTrue(stockExchange.getOpenPositions().get("AAPL").isEmpty());

                // Check correctness of recording the result of the trade in the Result Object.
                assertEquals(stockExchange.getResult().getAbsPerformance(), 19800000);
                assertEquals(stockExchange.getResult().getRelPerformance(), 0.0198);

                // Populate the currentPrices HashMap by requesting the first DataPoint of
                // Exchange.
                // This is necessary to calculate e.g. the current value of the portfolio
                // because it calculates the value of the whole portfolio based on the latest
                // available price.
                stockExchange.next();
                stockExchange.marketOrder("AAPL", 1000, 100, 7654321);
                stockExchange.marketOrder("MSFT", 1000, 100, 7654321);
                assertEquals(stockExchange.getCurrentPortfolioValue(), 304550.0);

                // Test marketClearPosition which essentially calls a marketOrder with the
                // latest available price.
                Double currentAbsReturn = stockExchange.getResult().getAbsPerformance();
                stockExchange.marketClearPosition("AAPL");
                stockExchange.marketClearPosition("MSFT");
                assertEquals(stockExchange.getResult().getAbsPerformance(), currentAbsReturn + 304550.0);
                assertEquals(stockExchange.getOpenPositionAmounts().get("AAPL"), 0);
                assertEquals(stockExchange.getOpenPositionAmounts().get("MSFT"), 0);

                // Test opening and closing a short position (full neutralization).
                stockExchange.marketOrder("AAPL", -1000, 100, 7654321);
                currentAbsReturn = stockExchange.getResult().getAbsPerformance();
                stockExchange.marketOrder("AAPL", 1000, 10, 7654321);
                assertEquals(stockExchange.getResult().getAbsPerformance(), currentAbsReturn + 90000.0);

                // Test opening and closing a short position (partial neutralization).
                stockExchange.marketOrder("AAPL", -2000, 100, 7654321);
                currentAbsReturn = stockExchange.getResult().getAbsPerformance();
                stockExchange.marketOrder("AAPL", 1000, 10, 7654321);
                assertEquals(stockExchange.getResult().getAbsPerformance(), currentAbsReturn + 90000.0);
                assertEquals(stockExchange.getOpenPositionAmounts().get("AAPL"), -1000);

                stockExchange.marketClearPosition("AAPL");

                // Test opening and closing a long position (full neutralization).
                stockExchange.marketOrder("AAPL", 1000, 10, 7654321);
                currentAbsReturn = stockExchange.getResult().getAbsPerformance();
                stockExchange.marketOrder("AAPL", -1000, 100, 7654321);
                assertEquals(stockExchange.getResult().getAbsPerformance(), currentAbsReturn + 90000.0);

                // Test opening and closing a long position (partial neutralization).
                stockExchange.marketOrder("AAPL", 2000, 10, 7654321);
                currentAbsReturn = stockExchange.getResult().getAbsPerformance();
                stockExchange.marketOrder("AAPL", -1000, 100, 7654321);
                assertEquals(stockExchange.getResult().getAbsPerformance(), currentAbsReturn + 90000.0);
                assertEquals(stockExchange.getOpenPositionAmounts().get("AAPL"), 1000);

                stockExchange.marketClearPosition("AAPL");
                assertEquals(stockExchange.getOpenPositionAmounts().get("AAPL"), 0);
        }

}
