package com.backt4j.data;

/***
 * <p>A {@code DataPoint} is the smallest entity of data in the backt4j framework.</p>
 * <p>This is what {@link com.backt4j.core.Exchange} will emit, if its {@code next} Method is called.
 * It could be a price aggregate (e.g. 1min) with 
 * the typical "high", "low", "open", "close" and "volume" members.</p>
 * <p>Multiple instances that are a {@code DataPoint} are usually held by a {@link Data} instance. 
 *  Such a {@link Data} instance is then emitted by an {@link com.backt4j.core.Exchange}.<p>
 */
public interface DataPoint {}
