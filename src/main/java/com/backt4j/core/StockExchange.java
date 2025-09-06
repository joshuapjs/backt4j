package com.backt4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.backt4j.data.PriceDataPoint;

/***
 * The {@code StockExchange} class is a simple implementation for doing simple backtests with preferably stocks.
 * It might work with similar assets as well but it is designed for Stocks (e.g. it won't work for Options or Futures).
 * You can use it to run quick backtests for a first impression of a signal (without considreation of trading costs because
 * it would introduce way more complexity).
 */
public class StockExchange extends Exchange {

    /***
     * The value of the Entries in {@code openPositions} are specified as as List with two elements: 
     * The first value is the amount of shares of the Position. It can be expected that this value is
     * always a whole number so {@code amount.intValue()} will not result in information loss.
     * The second value is the price the shares where bought at. This oversimplifies dramatically how transactions 
     * work. It is therefore encouraged to adjust this in your own implementation to consider different prices.
     */
    private HashMap<String, List<Double>> openPositions;
    /***
     * The {@code accountValue} specifies the exact initial amount of the account at the Exchange. it is used to calculate
     * based on the absolute realised return, the relative realised performance.
     */
    public final Double accountValue;
    /***
     * The {@code currentAccountValue} is the (current) capital available, so the result of the accountValue and Transactions.
     */
    private Double currentAccountValue;
    /***
     * The {@code results } object will keep track of the performance and be accessible to {@link com.backt4j.core.Backtest} for displaying it.
     */
    Results results;


    /***
     * A record to track the progress of the trades being made.
     * The amount variable will tell whether or not its meant to be a short or long trade.
     */
    private record Transaction(String ticker, Double amount, Double price, Integer timeStamp) {};
    private List<Transaction> transactions;
    private List<Results> resultsSeries;

    public StockExchange(Double budget) {
        openPositions = new HashMap<>();
        results = new Results();
        accountValue = budget;
        currentAccountValue = accountValue;
    }

    /***
     * Method for the Strategy object to trade with the StockExchange. 
     * Every specified price will be granted, so no slippage is taken into account. A long position is take if the amount variable
     * has a positive sign. Vice versa a short position can be taken by specifying an amount with a negative sign.
     * As expected each position needs to be cleared by an order in the opposite direction. For convenience there is marketClearPosition
     * Method to clear an open Position of a ticker.
     * 
     * @param ticker the ticker of the stock to trade.
     * @param amount is either positive or negative whether the trade should be long or short respectively.
     * @param price the current price of the asset which resultet in the transaction being made.
     * @param timeStamp the timestamp of the price from the current PriceDataPoint.
     * @return if successfull returns 0, if not 1 is returned. A trade is unsuccessfull if there is not enough budget to make it.
     */
    public int marketOrder(String ticker, Double amount, Double price, Integer timeStamp) {
        if (amount * price * -1 + currentAccountValue < 0) {
            return 1;
        }
        transactions.add(new Transaction(ticker, amount, price, timeStamp));

        Integer orderSign = Integer.signum(amount.intValue());
        Double currentAmount = openPositions.get(ticker).get(0);
        Double currentPrice = openPositions.get(ticker).get(1);

        // check whether the new order changes the openposition to zero or switches the sign
        // resulting in an implicit clearance of the position. In the second case it makes sense to
        // handle the order as two second ones, one being the one clearing the position and the other 
        // being the new position in the other direction.
        if (currentAmount + amount == 0.00) {
            marketClearPosition(ticker);
        } else if (Integer.signum(currentAmount.intValue() + amount.intValue()) != orderSign) {
            // We first safe the amount from the non clearning position and then clear the open position.
            Double residualShareAmount = openPositions.get(ticker).get(0) + amount;
            marketClearPosition(ticker);
            // Next we create the new position, in order to record the performance correctly by calling marketBuy again.
            marketOrder(ticker, residualShareAmount, currentPrice, timeStamp);
        } else {
            Double weightedPrice = ((currentAmount * currentPrice) + (amount * price)) / (currentAmount + amount);
            List<Double> updatedPositionValues = new ArrayList<>();
            updatedPositionValues.add(currentAmount + amount);
            updatedPositionValues.add(weightedPrice);
            openPositions.put(ticker, updatedPositionValues);
            resultsSeries.add(results);
            currentAccountValue =- amount * price;
        }
        return 0;
    }

    /***
     * This method can be used to clear out an open position. It is always called by marketOrder in case an order clears out 
     * a position either by buying/selling the exact open amount of shares or an amount that would make the whole position from e.g.
     * long into a short position (and by that clearing the position and creating a new opposite position).
     * 
     * @param ticker the ticker specifies the stock being traded.
     */
    public void marketClearPosition(String ticker) {
        Double initialValue = openPositions.get(ticker).get(0) * openPositions.get(ticker).get(1);
        PriceDataPoint currentPrice = (PriceDataPoint) this.currentPrices.get(ticker);
        Double currentValue = currentPrice.open() * openPositions.get(ticker).get(0);
        Double tempAbsoluteReturn;
        Double tempRelativeReturn;
        if (openPositions.get(ticker).get(0) >= 0) {

            tempAbsoluteReturn = currentValue - initialValue;
            tempRelativeReturn = tempAbsoluteReturn / initialValue;

        } else {

            tempAbsoluteReturn = initialValue - currentValue;
            tempRelativeReturn = tempAbsoluteReturn / (initialValue * -1);

        }
        results.setAbsPerformance(results.getAbsPerformance() + tempAbsoluteReturn);
        results.setRelPerformance(results.getAbsPerformance() / accountValue);
        if (results.maxDrawdown > tempRelativeReturn) {
            results.setMaxDrawdown(tempRelativeReturn);
        }
        openPositions.remove(ticker);
        resultsSeries.add(results);
    }

    private static double calculateVolatility(List<Double> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("List must not be empty");
        }

        double mean = calculateMean(values);
        double variance = 0.0;

        for (double value : values) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= values.size();

        return Math.sqrt(variance);
    }

    private static double calculateMean(List<Double> values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    @Override
    void handleRunEnd() {
        List<Double> relativeReturnsList = new ArrayList<>();
        Double vol = calculateVolatility(relativeReturnsList);
        results.volatility = vol;
    }

    public HashMap<String, List<Double>> getOpenPositions() {
        return openPositions;
    }

}
