package com.backt4j.data;

import java.util.HashMap;
import java.util.List;

/***
 * <p>The main abstract class for all implementations of PriceData classes that encapsulate the parsing and other 
 * Operations necessary for the Backtest class to work.</p>
 * 
 * <p>A couple of words about the idea behind the methods:</p>
 * 
 * <p>The {@code init()} method parses the data from the given source (e.g. CSV-File, Excel-File, REST API, ...) 
 * and stores it in a classvariable, called values from now on, of type {@code HashMap<String,List<DatePoint>>}. 
 * The data can be accessed by calling {@code getValues()}, this means {@code getValues()} should return values. 
 * As mentioned the {@code getValues()} method makes your data accessible to the {@link com.backt4j.core.Backtest} class.</p>
 * 
 * <p>As you can see, the {@code values} class variable contains a {@code List} of {@code DataPoint}. Feel free to write your own 
 * record as already done in {@code PriceDataPoint}, implementing DataPoint, so that it can be used in {@code Data}.
 * implementation in a separate file in the {@code com.backt4j.Data} package as needed.</p>
 * 
 * <p>It is recommended, to implement id ({@code String}), size ({@code Integer}), {@code minDate} and 
 * {@code maxDate} ({@code Date}) classvariables. Those variables can be accessed by the respective getter methods. 
 * I recommend to specify {@code size} during the initialization.</p>
 * 
 * <p>All Endpoints  must be specified for your class to work with {@link com.backt4j.core.Backtest}.</p>
 * 
 */
public interface Data {

    /***
     * This method initializes the PriceData implementation, by parsing the data from the source given was 
     * the parameter sourceString and storing it in a class variable of type {@code HashMap<String,List<DatePoint>>}
     * so that it can be accessed by the {@code getValues()} method.
     * 
     * @param fileString The path to the file given as String.
     */
    public Data init(String sourceString) throws Exception;

    public String getId();

    public Integer size();

    public HashMap<String, List<DataPoint>> getValues() throws Exception;

}
