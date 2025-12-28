import java.io.File;

import org.junit.jupiter.api.Test;

import com.backt4j.core.Backtest;
import com.backt4j.core.StockExchange;
import com.backt4j.data.CSVData;
import com.backt4j.strategy.TestStrategy;

public class AppTest {

    @Test
    public void simpleDryRun() throws Exception {
        File resourcesDirectory = new File("src/test/resources");
        CSVData csvData = (CSVData) (new CSVData("csv-data-name"))
                .init(resourcesDirectory.getAbsolutePath() + "/testdata.csv");
        assert csvData.getValues() != null;
        StockExchange stockExchange = new StockExchange(1_000_000, csvData);
        TestStrategy testStrategy = new TestStrategy(1000.0, 0.01);
        Backtest backtest = new Backtest.Builder().add(stockExchange).add(testStrategy).build();
        backtest.run();
    }

}
