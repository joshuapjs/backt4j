# backt4j

A small customizable backtesting framework for Java.

# Getting Started

```java
CSVData priceData = (CSVData) new CSVData()
        .init(/* path to csv file */);

Backtest backtest = new Backtest.Builder()
        .add(new StockExchange(
            1_000_000, 
            priceData))
        .add(new TestStrategy(1000.0, 0.01))
        .build();

backtest.run();
```

# Overview

The framework is based on three primary parts that interact with each other:
- [Strategy](src/main/java/com/backt4j/strategy/Strategy.java): A `Strategy` implementation must contain the trading logic, the user of this framework wants to test. Such an implementation must specify what happens if a new [DataPoint](src/main/java/com/backt4j/data/DataPoint.java), e.g. new price data, is fed to `handleNewPrice()`. An example how this could be done can be viewed [here](src/main/java/com/backt4j/strategy/TestStrategy.java).
- [Exchange](src/main/java/com/backt4j/core/Exchange.java): The `Exchange` supplies the `Strategy` with a new [DataPoint](src/main/java/com/backt4j/data/DataPoint.java) whenever `next()` is called. On the other hand it should record trades made by the `Strategy` while at the same time calculate risk and performace related indicators. An example how this could be done can be viewed [here](src/main/java/com/backt4j/core/StockExchange.java).
- [Backtest](src/main/java/com/backt4j/core/Backtest.java): Both classes are connected through a [Connection](src/main/java/com/backt4j/core/Connection.java) class, which is a simple wrapper to organize a `Strategy` and `Exchange`. `Connection` instances are managed by the `Backtest` class.

