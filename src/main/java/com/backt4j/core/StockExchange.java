package com.backt4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.ArrayDeque;
import java.lang.Math;

import com.backt4j.data.PriceDataPoint;
import com.backt4j.data.Data;

/***
 * <p>
 * The {@code StockExchange} class is an implementation for doing simple backtests with stocks.
 * </p>
 * <p>
 * It might work with similar assets but was designed with stocks in mind. You can use it to run
 * quick backtests for a first impression of a signal.
 * </p>
 */
public class StockExchange extends Exchange {

    /***
     * The Entries in {@code openPositions} are Deques containing Transaction records that contain
     * each the amount and the price of a trade. By definition of a Queue the Transactions are
     * sorted by their arrival. As it is currently assumed that trades are always fullfilled with
     * the desired price, it can be guaranteed that each Transaction was a result from one trade.
     **/
    private HashMap<String, ArrayDeque<Transaction>> openPositions;
    /***
     * This {@link HashMap} stores, for each stock a position was created at some point, the total
     * amount of shares with the respective sign.
     */
    private HashMap<String, Integer> openPositionAmounts;
    /***
     * The {@code initialBudget} specifies the initial amount allocated to the account at the
     * Exchange.
     */
    private final double initialBudget;
    /***
     * The {@code remaininglBudget} specifies the remaining amount allocated to the account at the
     * Exchange. Which is available for further asset purchases. It also includes realised returns.
     * This variable should be used for bounds checking to determine if a trade is allowed or not.
     */
    private double remainingBudget;

    /***
     * The {@code results } object will keep track of the performance and be accessible to
     * {@link com.backt4j.core.Backtest} for displaying it.
     */
    Result results;

    /***
     * <p>
     * A record to track each trade that happened. It will be used to keep a history but also to
     * manage open positions.
     * </p>
     * <p>
     * The sign of the amount number will tell whether or not its meant to be a short or long trade.
     * </p>
     * <p>
     * Method calls of marketClearPosition will create a respective transaction as well.
     */
    public record Transaction(String ticker, int amount, double price, long timeStamp) {
    };

    private List<Transaction> transactions;

    public StockExchange(double budget, Data newData) {
        super(newData);
        openPositions = new HashMap<>();
        openPositionAmounts = new HashMap<>();
        results = new Result();
        initialBudget = budget;
        remainingBudget = budget;
        transactions = new ArrayList<>();
    }

    public StockExchange(int budget, Data newData) {
        super(newData);
        openPositions = new HashMap<>();
        openPositionAmounts = new HashMap<>();
        results = new Result();
        initialBudget = (double) budget;
        remainingBudget = (double) budget;
        transactions = new ArrayList<>();
    }

    /***
     * <p>
     * Method for the {@code Strategy} object to trade with the {@code StockExchange}.
     * </p>
     * <p>
     * Every specified price will be granted, so no slippage is taken into account. A long position
     * is taken if the amount variable has a positive sign. Vice versa a short position can be taken
     * by specifying an amount with a negative sign.
     * </p>
     * <p>
     * As expected each position needs to be cleared by an order inverse to the order being made
     * initially. For convenience there is {@code marketClearPosition} method to clear an open
     * position of a ticker.
     * </p>
     * 
     * @param ticker the ticker of the stock to trade.
     * @param amount is either positive or negative whether the trade should be long or short
     *        respectively.
     * @param price the current price of the asset which resultet in the transaction being made.
     * @param timeStamp the timestamp of the price from the current PriceDataPoint.
     * @return if successfull returns 0, if not 1 is returned. A trade is unsuccessfull if there is
     *         not enough budget to make it.
     */
    public int marketOrder(String ticker, int amount, double price, long timeStamp) {
        // Check if there is enough budget for the transaction.
        if (amount * price * -1 + remainingBudget < 0) {
            return 1;
        }

        // Transaction is legal because there is enough budget.
        Transaction orderTransaction = new Transaction(ticker, amount, price, timeStamp);
        transactions.add(orderTransaction);

        // In order to handle the next cases correctly we need to determine the signs of
        // Potfolio and order.
        int orderSign = Integer.signum(amount);
        boolean noPosition =
                openPositions.get(ticker) == null || openPositions.get(ticker).size() == 0;
        // Handle the case where we have no positions for the ticker at all.
        if (noPosition) {
            assert openPositionAmounts.get(ticker) == null || openPositionAmounts.get(ticker) == 0;
            openPositionAmounts.put(ticker, amount);
            remainingBudget -= (amount * price);
            if (openPositions.get(ticker) == null) {
                openPositions.put(ticker, new ArrayDeque<>());
                openPositions.get(ticker).addFirst(orderTransaction);
            } else {
                openPositions.get(ticker).addFirst(orderTransaction);
            }
            return 0;
        }
        int portfolioSign = Integer.signum(openPositionAmounts.get(ticker));

        while (amount != 0) {
            // First we handle the case where a long or short positions is just increased.
            if (orderSign == portfolioSign) {
                // As there is a position, the position volume must be adjusted.
                int currentOpenPositionAmount = openPositionAmounts.get(ticker);
                currentOpenPositionAmount += amount;
                openPositionAmounts.put(ticker, currentOpenPositionAmount);
                // Next the budget has to be adjusted.
                remainingBudget -= (amount * price);
                openPositions.get(ticker).addFirst(orderTransaction);
                amount = 0;
                // Secondly we handle sell of a long position.
            } else if (orderSign < portfolioSign) {
                // A sell leads to neutralization of older positions.
                Transaction firstExecutedTransaction = openPositions.get(ticker).peekLast();
                // We therefore check if the firstExecutedTransaction will be consumed entirely.
                int amountLeft = firstExecutedTransaction.amount() > Math.abs(amount)
                        ? firstExecutedTransaction.amount() + amount
                        : 0;

                if (amountLeft == 0) {
                    // Adjust the amount by the amount of the firstExecutedTransaction.
                    amount += firstExecutedTransaction.amount();

                    // As there is a position, the position volume must be adjusted.
                    int currentOpenPositionAmount = openPositionAmounts.get(ticker);
                    currentOpenPositionAmount -= firstExecutedTransaction.amount();
                    openPositionAmounts.put(ticker, currentOpenPositionAmount);

                    // Next the budget has to be adjusted.
                    // Closing of a position always leads to adjustment by tradePerformance.
                    Double tradePerformance = firstExecutedTransaction.amount()
                            * (price - firstExecutedTransaction.price());
                    remainingBudget += tradePerformance;

                    // If the old trade was consumed entirely by the order we can remove it.
                    openPositions.get(ticker).removeLast();

                    // As a position was closed we have to update the results Object.
                    updateResults(tradePerformance);
                } else {
                    // As there is a position, the position volume must be adjusted.
                    int currentOpenPositionAmount = openPositionAmounts.get(ticker);
                    currentOpenPositionAmount += amount;
                    openPositionAmounts.put(ticker, currentOpenPositionAmount);

                    // Next the budget has to be adjusted.
                    // Closing of a position always leads to adjustment by tradePerformance.
                    Double tradePerformance =
                            Math.abs(amount) * (price - firstExecutedTransaction.price());
                    remainingBudget += tradePerformance;

                    // The old trade was NOT consumed entirely by the order so we have to update it.
                    Transaction updatedTransaction = new Transaction(
                            firstExecutedTransaction.ticker(), amountLeft,
                            firstExecutedTransaction.price(), firstExecutedTransaction.timeStamp());
                    openPositions.get(ticker).removeLast();
                    openPositions.get(ticker).addLast(updatedTransaction);

                    // As a position was closed we have to update the results Object.
                    updateResults(tradePerformance);

                    // The order partially consumed the position and is zero now.
                    amount = 0;
                }
                // Lastly we handle buy of a short position.
            } else {
                // A buy leads to neutralization of older positions.
                Transaction firstExecutedTransaction = openPositions.get(ticker).peekLast();
                // We therefore check if the firstExecutedTransaction will be consumed entirely.
                int amountLeft = Math.abs(firstExecutedTransaction.amount()) > amount
                        ? firstExecutedTransaction.amount() + amount
                        : 0;

                if (amountLeft == 0) {
                    // Adjust the amount by the amount of the firstExecutedTransaction.
                    amount += firstExecutedTransaction.amount();

                    // As there is a position, the position volume must be adjusted.
                    int currentOpenPositionAmount = openPositionAmounts.get(ticker);
                    currentOpenPositionAmount -= firstExecutedTransaction.amount();
                    openPositionAmounts.put(ticker, currentOpenPositionAmount);

                    // Next the budget has to be adjusted.
                    // Closing of a position always leads to adjustment by tradePerformance.
                    Double tradePerformance = Math.abs(firstExecutedTransaction.amount())
                            * (firstExecutedTransaction.price() - price);
                    remainingBudget += tradePerformance;

                    // If the old trade was consumed entirely by the order we can remove it.
                    openPositions.get(ticker).removeLast();

                    // As a position was closed we have to update the results Object.
                    updateResults(tradePerformance);
                } else {
                    // As there is a position, the position volume must be adjusted.
                    int currentOpenPositionAmount = openPositionAmounts.get(ticker);
                    currentOpenPositionAmount += amount;
                    openPositionAmounts.put(ticker, currentOpenPositionAmount);

                    // Next the budget has to be adjusted.
                    // Closing of a position always leads to adjustment by tradePerformance.
                    Double tradePerformance =
                            amount * (firstExecutedTransaction.price() - price);
                    remainingBudget += tradePerformance;

                    // The old trade was NOT consumed entirely by the order so we have to update it.
                    Transaction updatedTransaction = new Transaction(
                            firstExecutedTransaction.ticker(), amountLeft,
                            firstExecutedTransaction.price(), firstExecutedTransaction.timeStamp());
                    openPositions.get(ticker).removeLast();
                    openPositions.get(ticker).addLast(updatedTransaction);

                    // As a position was closed we have to update the results Object.
                    updateResults(tradePerformance);

                    // The order partially consumed the position and is zero now.
                    amount = 0;

                }
            }
        }
        return 0;
    }

    private void updateResults(Double absPerformance) {
        results.setAbsPerformance(results.getAbsPerformance() + absPerformance);
        results.setRelPerformance(results.getAbsPerformance() / initialBudget);
        Double maxDrawDownUpdate = absPerformance < results.getMaxDrawdown() ? absPerformance
                : results.getMaxDrawdown();
        results.setMaxDrawdown(maxDrawDownUpdate);
        List<Double> newPerformanceSeries = results.getPerformanceSeries();
        newPerformanceSeries.add(results.getRelPerformance());
        results.setPerformanceSeries(newPerformanceSeries);
    }

    /***
     * <p>
     * This method can be used to clear out an open position.
     * </p>
     * <p>
     * It is always called by {@code marketOrder} in case an order clears a position by
     * buying/selling the exact open amount of shares <b>or</b> an amount that would make the whole
     * position from e.g. long into a short position (and by that clearing the position and creating
     * a new opposite position).
     * </p>
     * 
     * @param ticker the ticker specifies the stock being traded.
     */
    public void marketClearPosition(String ticker) {
        ArrayDeque<Transaction> allOrders = openPositions.get(ticker);
        if (allOrders == null) {
            return;
        }
        for (Transaction t : allOrders) {
            PriceDataPoint currentDataPoint = (PriceDataPoint) currentPrices.get(ticker);
            marketOrder(ticker, t.amount() * -1, currentDataPoint.open(), 
                    currentDataPoint.window_start().getTime());
        }
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /***
     * <p>
     * Returns all current {@code openPositions}.
     * </p>
     * 
     * <p>
     * The Entries in {@code openPositions} are Deques containing Transaction records that contain
     * the volume and price of each trade. As it is currently assumed that trades are always
     * fullfilled with the desired price, it can be guaranteed that each Transaction was a result
     * from one trade.
     * </p>
     **/
    public HashMap<String, ArrayDeque<Transaction>> getOpenPositions() {
        return openPositions;
    }

    /***
     * <p>
     * Returns all current {@code openPositionAmounts}, which is essentially a HashMap with the
     * tickers as keys and the amount as value.
     * </p>
     */
    public HashMap<String, Integer> getOpenPositionAmounts() {
        return openPositionAmounts;
    }

    /***
     * Returns the {@code initialBudget}, the initially amount that was allocated to the account at
     * the Exchange.
     */
    @Override
    public Double getInitialBudget() {
        return initialBudget;
    }

    /***
     * Returns {@code remainingBudget} which is {@code initialBudget} + all transaktions that
     * happened since the initialization of the budget.
     * 
     * The {@code remainingBudget} specifies the remaining amount allocated to the account at the
     * Exchange. Which is available for further asset purchases. It also includes realised returns.
     * This variable should be used for bounds checking to determine if a trade is allowed or not.
     */
    @Override
    public Double getRemainingBudget() {
        return remainingBudget;
    }

    /***
     * Calculates the current portfolio value based on the the latest {@link PriceDataPoint} and the
     * {@code openPositions}, by multiplying the respective amounts with the prices of the assets.
     */
    @Override
    public Double getCurrentPortfolioValue() {
        Double currentPortfolioValue = 0.0;
        if (!openPositions.isEmpty()) {
            for (String ticker : openPositions.keySet()) {
                if (currentPrices.get(ticker) == null) {
                    continue;
                }
                Double currentPrice = ((PriceDataPoint) currentPrices.get(ticker)).open();
                ArrayDeque<Transaction> openTransactions = openPositions.get(ticker);
                if (openTransactions == null) {
                    continue;
                }
                if (openTransactions.isEmpty()) {
                    continue;
                }
                for (Transaction t : openTransactions) {
                    Integer volume = t.amount();
                    Double buyIn = t.price();
                    currentPortfolioValue += volume * (currentPrice - buyIn);
                }
            }
        }
        return currentPortfolioValue;
    }

    /***
     * Returns the current {@link Result} object on the Exchange.
     * 
     * The {@code results } object will keep track of the performance and be accessible to
     * {@link com.backt4j.core.Backtest} for displaying it.
     */
    @Override
    public Result getResult() {
        return results;
    }

}
