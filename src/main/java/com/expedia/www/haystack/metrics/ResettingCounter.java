package com.expedia.www.haystack.metrics;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation backed by an {@link java.util.concurrent.atomic.AtomicLong}.
 * The value is the total count since the last sampling point; that is, the value is reset when getValue() is called.
 * This counter differs from {@link com.netflix.servo.monitor.BasicCounter} in that its getValue() method returns the
 * count, not a rate, and in the value reset performed by getValue().
 */
public final class ResettingCounter extends AbstractMonitor<Number> implements Counter {
    private final AtomicLong count = new AtomicLong();

    /**
     * Create a new instance with the specified configuration.
     *
     * @param config the counter configuration
     */
    @SuppressWarnings("WeakerAccess")
    public ResettingCounter(MonitorConfig config) {
        super(config.withAdditionalTag(DataSourceType.COUNTER));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        count.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(long amount) {
        count.getAndAdd(amount);
    }

    /**
     * {@inheritDoc}
     * Note that this method resets the counter to 0.
     */
    @Override
    public Number getValue(int pollerIndex) {
        return count.getAndSet(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ResettingCounter)) {
            return false;
        }
        ResettingCounter m = (ResettingCounter) obj;
        return config.equals(m.getConfig()) && count.get() == m.count.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = config.hashCode();
        long n = count.get();
        result = 31 * result + (int) (n ^ (n >>> 32));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ResettingCounter{config=" + config + ", count=" + count.get() + '}';
    }
}
