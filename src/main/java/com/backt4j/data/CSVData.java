package com.backt4j.data;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import java.sql.Timestamp;

import com.opencsv.CSVReader;

/***
 * <p>A Class for parsing price data for each a Flat Files with time series data for stocks.
 * This class follows the Polygon.io Flat Files data format, with the following header:</p>
 * 
 * <p>ticker (str) volume (long) open (double) close (double) high (double) low (double) 
 * window_start (long) transactions (long)</p>
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

    /***
     * HashMap containing all the parsed financial data. Each asset receives its assigned List
     * of Data Records.
     */
    private HashMap<String, List<DataPoint>> values;

    public CSVData(String IdArg) {
        Id = IdArg;
        values = new HashMap<>();
    }

    public CSVData() {
        // Supply default Id if none is given.
        Id = "csv-element-" + (new Timestamp(System.currentTimeMillis())).getTime(); 
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
    public Data init(String fileString) throws Exception {

        // Parse the CSV file.
        Path filePath = Paths.get(fileString);

        // The amount of prices available is given by the amount of lines without the header.
        if (filePath.toFile().isFile()) {

            HashMap<String, List<DataPoint>> parsedValues = parseFile(fileString);
            extendValues(parsedValues);

       } else if (filePath.toFile().isDirectory()) {

            try (Stream<Path> paths = Files.walk(Paths.get(fileString))) {
                Object[] allFiles = (Object[]) paths.filter(Files::isRegularFile).toArray();
                for (int i=0; i<allFiles.length; i++) {
                    Path f = (Path) allFiles[i];
                    extendValues(parseFile(f.toString()));
                }
            } catch (Exception e) {
                throw new Exception(e);
            }

       }
       return this;
    }

    private void extendValues(HashMap<String, List<DataPoint>> valuesExtention) {
            for (String key : valuesExtention.keySet()) {
                if (values.containsKey(key)) {
                    values.get(key).addAll(valuesExtention.get(key));
                } else {
                    values.put(key, valuesExtention.get(key));
                }
            }
    }

    private HashMap<String, List<DataPoint>> parseFile(String pathString) throws Exception {

        // Parse the CSV file.
        Path filePath = Paths.get(pathString);
        List<String[]> allLines = new ArrayList<>();
        HashMap<String, List<DataPoint>> parsedData = new HashMap<>();

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
                parsedData.get(ticker).add(new PriceDataPoint(
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
                parsedData.put(ticker, records);
            } catch (Exception e) {
                System.out.println("An Exception other than NullPointException occured: " + e);
            }
        }

        if (this.size == null) {
            this.size = allLines.size() - 1;
        } else {
            if (this.size != allLines.size() - 1) {
                throw new Exception("Amount of lines in a CSV file does not match with " +
                "the amount of lines that were already parsed.");
            }
        }

        return parsedData;

    }

    @Override
    public String getId() {
        return Id;
    }

    @Override
    public Integer size() {
        return size;
    }

    @Override
    public HashMap<String, List<DataPoint>> getValues() throws Exception {
        if (values.isEmpty()) {
            throw new Exception("No Data was assigned CSVData Object. Make sure to call init() before trying to access values.");
        } else {
            return values;
        }
    }

};
