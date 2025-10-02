# backt4j

A small framework for backtesting for Java.

# Overview

The whole framework is based on three primary parts that interact with each other. We have on the first side the strategy and on the other side the exchange. 
The strategy is the trading logic, the user of this framework needs to implement. Every Strategy must extend the Strategy class and thereby specify what should happen if a new price is fed into the Strategy. 
On the other hand we have the exchange. The purpose of the exchange is on the one hand to supply data to the Strategy and on the other hand to record trades from the strategy while at the same time risk and performace related indicators are calculated based on the action initiated by the strategy.
Both classes are connected through a Connection class. This means every instance of Exchange and Strategy must receive a Connection during initialization.