package com.backt4j.data;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.lang.Exception;

import com.opencsv.CSVReader;

/***
 * <p>A Class for parsing price data for each a Flat Files with time series data for stocks.
 * This class follows the Polygon.io Flat Files data format, with the following header:</p>
 * 
 * <p>ticker volume open close high low window_start transactions</p>
 * 
 * <p>The Data is stored in a HashMap, where we have for each stock a List of DataPoint records.</p>
 * <p>Each DataPoint has a price and a timeStamp in this implementation.</p>
 * <p>Currently all data for one Exchange must be in one file.</p>
 * 
 * <p><b>CAUTION:</b></p>
 * <p>The class expects the Data to be well formatted. 
 * This means each stock must have the exact same amount of DataPoints.
 * Beyond that make sure that elements are already sorted in the way the Strategy should be tested on them.</p>
 * 
 * <p>The parsing of the CSV data starts with the init() Method.</p>
 */
public class CSVData implements Data {
    
    public String Id;  // E.g. Date the Data was collected.
    public Integer size;
    public Date minDate;  
    public Date maxDate;

    /***
     * HashMap containing all the parsed financial data. Each asset receives its assigned List
     * of Data Records.
     */
    public HashMap<String, List<DataPoint>> values;

    public CSVData(Date startDateArg, Date endDateArg, String IdArg) {
        minDate = startDateArg;
        maxDate = endDateArg;
        Id = IdArg;
        values = new HashMap<>();
    }

    /***
     * Takes a Path to a CSV File an reads all Lines with opencsv.CSVReader.readAll().
     * 
     * @param file A Path Object to the desired CSV File.
     * @return All lines are returned in a single List<String[]> Object.
     * @throws Exception If the Path is invalid, an Exception will be thrown.
     */
    public List<String[]> readAllLines(Path file) throws Exception {
        try (Reader reader = Files.newBufferedReader(file)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                return csvReader.readAll();
            }
        }
    }

    /***
     * Initialization of the Data Class by parsing the CSV File.
     * Then set up of the values classvariable.  
     * 
     * @param fileString The Path to the file given as String.
     */
    @Override
    public void init(String fileString) {

        // Parse the CSV file.
        Path filePath = Paths.get(fileString);
        List<String[]> allLines = new ArrayList<>();
        // The amount of prices available is given by the amount of lines without the header.
        size = allLines.size() - 1;
        try {
            allLines = readAllLines(filePath);
        } catch (Exception e) {
            System.out.println("An Exception occured" + e.getStackTrace());
        }
        Iterator<String[]> allLinesIterator = allLines.iterator();

        // We skip the header.
        allLinesIterator.next();

        while (allLinesIterator.hasNext()) {
            String[] line = allLinesIterator.next();
            String ticker = line[0];

            // Handle values for known tickers and new ones accordingly.
            try {
                // Assign opening price as price and window_start as timeStamp.
                values.get(ticker).add(new PriceDataPoint(
                    ticker, 
                    (Integer) Integer.parseInt(line[1]), 
                    Double.parseDouble(line[2]), 
                    Double.parseDouble(line[3]), 
                    Double.parseDouble(line[4]), 
                    Double.parseDouble(line[5]), 
                    new Date(Long.parseLong(line[6])),
                    (Integer) Integer.parseInt(line[7])
                    ));
            } catch (NullPointerException emptyList) {
                // The ticker is new and we create a List with the first encountered DataPoint in it.
                DataPoint dataPoint = new PriceDataPoint(
                    ticker, 
                    (Integer) Integer.parseInt(line[1]), 
                    Double.parseDouble(line[2]), 
                    Double.parseDouble(line[3]), 
                    Double.parseDouble(line[4]), 
                    Double.parseDouble(line[5]), 
                    new Date(Long.parseLong(line[6])),
                    (Integer) Integer.parseInt(line[7])
                    );
                List<DataPoint> records = new ArrayList<>();
                records.add(dataPoint);
                values.put(ticker, records);
            } catch (Exception e) {
                System.out.println("An Exception other than NullPointException occured: " + e);
            }
        }
            
        // Uncomment for testing purposes to inspect an Excample for AAPL.
        System.out.println(values.get("AAPL"));

    }

    @Override
    public String getId() {
        return Id;
    }

    @Override
    public Date getMinDate() {
        return minDate;
    }

    @Override
    public Date getMaxDate() {
        return maxDate;
    }

    @Override
    public Integer size() {
        return size;
    }

    @Override
    public HashMap<String, List<DataPoint>> getValues() throws Exception {
        if (values.isEmpty()) {
            throw new Exception("No Data assigned. Please make sure to call init() before accessing values.");
        } else {
            return values;
        }
    }

};
