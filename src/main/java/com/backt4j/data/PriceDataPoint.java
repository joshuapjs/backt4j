package com.backt4j.data;

import java.util.Date;

/***
 * A Data Record representing a single data point in a time series.
 * 
 * ticker volume open close high low window_start transactions
 * 
 * @param id Given id of the asset.
 * @param volume Traded volume of the asset.
 * @param window_start Current point in time given as a java.util.Date instance.
 * @param transactions Amount of transactions in the time window.
 * @return returns a DataPoint record.
 */
public record PriceDataPoint (String id, 
                        Integer volume, 
                        Double open, 
                        Double close, 
                        Double high, 
                        Double low, 
                        Date window_start, 
                        Integer transactions) implements DataPoint {}; 
