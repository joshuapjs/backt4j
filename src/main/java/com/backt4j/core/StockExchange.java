package com.backt4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.backt4j.data.PriceDataPoint;
import com.backt4j.data.Data;

/***
 * <p>The {@code StockExchange} class is an implementation for doing simple backtests with stocks.</p>
 * <p>It might work with similar assets but was designed with stocks in mind. 
 * You can use it to run quick backtests for a first impression of a signal.</p>
 */
public class StockExchange extends Exchange {

    /***
     * <p>The Entries in {@code openPositions} are specified as List of two elements:</p>
     * <ul>
     * <li>The <b>first value is the amount of shares of the Position</b>. It is expected that this value is
     * always a whole number so {@code amount.intValue()} must work without information loss. 
     * Trading of fractions is thereby not implemented.</li>
     * <li>The <b>second value is the price the shares where bought at</b>. This oversimplifies dramatically how transactions 
     * work. It is therefore encouraged to adjust this in your own implementation to consider different prices.</li>
     * </ul>
     */
    private HashMap<String, List<Double>> openPositions;
    /***
     * The {@code initialBudget} specifies the initial amount allocated to the account at the Exchange.
     */
    public final Double initialBudget;
    /***
     * The {@code remaininglBudget} specifies the remaining amount allocated to the account at the Exchange. 
     * Which is available for further asset purchases. It also includes realised returns.
     * This variable should be used for bounds checking to determine if a trade is allowed or not.
     */
    public Double remainingBudget;

    /***
     * The {@code results } object will keep track of the performance and be accessible to {@link com.backt4j.core.Backtest} for displaying it.
     */
    Result results;

    /***
     * <p>A record to track each trade that happened.</p>
     * <p>The sign of the amount number will tell whether or not its meant to be a short or long trade.</p>
     * <p>Method calls of marketClearPosition will create a respective transaction as well.
     */
    public record Transaction(String ticker, Double amount, Double price, Long timeStamp) {};
    private List<Transaction> transactions;
    private List<Result> resultsSeries;

    public StockExchange(Double budget, Data newData) {
        super(newData);
        openPositions = new HashMap<>();
        results = new Result();
        initialBudget = budget;
        remainingBudget = budget;
        transactions = new ArrayList<>();
        resultsSeries = new ArrayList<>();
    }

    public StockExchange(Integer budget, Data newData) {
        super(newData);
        openPositions = new HashMap<>();
        results = new Result();
        initialBudget = Double.valueOf(budget);
        remainingBudget = Double.valueOf(budget);
        transactions = new ArrayList<>();
        resultsSeries = new ArrayList<>();
    }

    /***
     * <p>Method for the {@code Strategy} object to trade with the {@code StockExchange}.</p>
     * <p>Every specified price will be granted, so no slippage is taken into account. A long position is taken if the amount variable
     * has a positive sign. Vice versa a short position can be taken by specifying an amount with a negative sign.</p>
     * <p>As expected each position needs to be cleared by an order inverse to the order being made initially. For convenience there is {@code marketClearPosition}
     * method to clear an open position of a ticker.</p>
     * 
     * @param ticker the ticker of the stock to trade.
     * @param amount is either positive or negative whether the trade should be long or short respectively.
     * @param price the current price of the asset which resultet in the transaction being made.
     * @param timeStamp the timestamp of the price from the current PriceDataPoint.
     * @return if successfull returns 0, if not 1 is returned. A trade is unsuccessfull if there is not enough budget to make it.
     */
    public int marketOrder(String ticker, Double amount, Double price, Long timeStamp) {
        if (amount * price * -1 + remainingBudget < 0) {
            return 1;
        }
        transactions.add(new Transaction(ticker, amount, price, timeStamp));

        Integer orderSign = Integer.signum(amount.intValue());
        Double currentAmount;
        Double currentPrice;
        currentAmount = openPositions.get(ticker) != null ? openPositions.get(ticker).get(0) : 0.0;
        currentPrice = openPositions.get(ticker) != null ? openPositions.get(ticker).get(1) : 0.0;

        // check whether the new marketorder changes the open position to zero or switches the sign,
        // resulting in an implicit clearance of the position. In the second case it makes sense to
        // handle the order as two separate ones, one being the one clearing the position and the other 
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
            remainingBudget -= amount * price;
        }
        return 0;
    }

    /***
     * <p>This method can be used to clear out an open position.</p>
     * <p>It is always called by {@code marketOrder} in case an order clears a position by buying/selling the exact open amount of shares
     * <b>or</b> an amount that would make the whole position from e.g. long into a short position (and by that clearing the position and creating a new opposite position).</p>
     * 
     * @param ticker the ticker specifies the stock being traded.
     */
    public void marketClearPosition(String ticker) {
        Double initialValue = openPositions.get(ticker).get(0) * openPositions.get(ticker).get(1);
        PriceDataPoint currentPrice = (PriceDataPoint) this.currentPrices.get(ticker);
        Double currentValue = currentPrice.open() * openPositions.get(ticker).get(0);
        // In the case that we try to even out a short position we want to deduct that price of 
        // Buying back the shares from our remaining budget.
        remainingBudget += currentValue;
        transactions.add(new Transaction(ticker, 
                                        openPositions.get(ticker).get(0), 
                                        currentPrice.open(),
                                        null));
        Double tempAbsoluteReturn;
        Double tempRelativeReturn;
        if (openPositions.get(ticker).get(0) >= 0) {

            tempAbsoluteReturn = currentValue - initialValue;
            tempRelativeReturn = tempAbsoluteReturn / initialValue;

        } else {

            tempAbsoluteReturn = initialValue - currentValue;
            tempRelativeReturn = tempAbsoluteReturn / (initialValue * -1);

        }
        Double currentAbsPerf = results.getAbsPerformance() != null ? results.getAbsPerformance() : 0.0;
        results.setAbsPerformance(currentAbsPerf + tempAbsoluteReturn);
        results.setRelPerformance((currentAbsPerf + tempAbsoluteReturn) / initialBudget);

        Double currentMaxDraw = results.getMaxDrawdown() != null ? results.getMaxDrawdown() : 0.0;
        if (tempRelativeReturn < 0.0 && tempRelativeReturn < currentMaxDraw) {
            results.setMaxDrawdown(tempRelativeReturn);
        }
        if (results.getMaxDrawdown() == null) {
            results.setMaxDrawdown(currentMaxDraw);
        }
        openPositions.remove(ticker);
        resultsSeries.add(results);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public HashMap<String, List<Double>> getOpenPositions() {
        return openPositions;
    }

    @Override
    public Double getInitialBudget() {
        return initialBudget;
    }

    @Override
    public Double getRemainingBudget() {
        return remainingBudget;
    }

    @Override
    public Double getCurrentPortfolioValue() {
        Double currentPortfolioValue = 0.0;
        for (String ticker : openPositions.keySet()) {
            Double sharesBought= openPositions.get(ticker).get(0);
            Double buyIn = openPositions.get(ticker).get(1);
            Double currentPrice = ((PriceDataPoint) currentPrices.get(ticker)).open();
            currentPortfolioValue += sharesBought * (currentPrice - buyIn);
        }
        return currentPortfolioValue;
    }

    @Override
    public Result getResult() {
        return results;
    }

}
