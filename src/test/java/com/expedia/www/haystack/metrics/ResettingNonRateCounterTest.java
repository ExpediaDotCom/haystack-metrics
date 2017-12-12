package com.expedia.www.haystack.metrics;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static com.netflix.servo.annotations.DataSourceType.COUNTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ResettingNonRateCounterTest {
    private static final Random RANDOM = new Random();
    private static final String COUNTER_NAME = RANDOM.nextLong() + "COUNTER_NAME";
    private static final int POLLER_INDEX = RANDOM.nextInt();
    private static final long AMOUNT = RANDOM.nextLong();

    private MonitorConfig monitorConfig;
    private ResettingNonRateCounter resettingNonRateCounter;

    @Before
    public void setUp() {
        monitorConfig = MonitorConfig.builder(COUNTER_NAME).build();
        resettingNonRateCounter = new ResettingNonRateCounter(monitorConfig);
    }

    @Test
    public void testConstructor() {
        final TagList tags = resettingNonRateCounter.getConfig().getTags();
        assertEquals(1, tags.size());
        for (Tag tag : tags) {
            assertEquals(DataSourceType.KEY, tag.getKey());
            assertEquals(COUNTER.toString(), tag.getValue());
        }
    }

    @Test
    public void testIncrement() {
        resettingNonRateCounter.increment();

        assertEquals(1L, resettingNonRateCounter.getValue(POLLER_INDEX));
    }

    @Test
    public void testIncrementWithValue() {
        resettingNonRateCounter.increment(AMOUNT);

        assertEquals(AMOUNT, resettingNonRateCounter.getValue(POLLER_INDEX));
    }

    @Test
    public void testGetValue() {
        resettingNonRateCounter.increment(AMOUNT);

        resettingNonRateCounter.getValue(POLLER_INDEX);
        assertEquals(0L, resettingNonRateCounter.getValue(POLLER_INDEX));
    }

    @Test
    public void testEquals() {
        assertEquals(resettingNonRateCounter, resettingNonRateCounter);
        assertNotEquals(resettingNonRateCounter, null);
        assertNotEquals(resettingNonRateCounter, "Object that is not an instance of ResettingNonRateCounter");
        final ResettingNonRateCounter otherResettingNonRateCounter = new ResettingNonRateCounter(monitorConfig);
        assertEquals(resettingNonRateCounter, otherResettingNonRateCounter);
        otherResettingNonRateCounter.increment();
        assertNotEquals(resettingNonRateCounter, otherResettingNonRateCounter);
        assertNotEquals(resettingNonRateCounter, new ResettingNonRateCounter(MonitorConfig.builder("").build()));
    }

    @Test
    public void testHashCode() {
        resettingNonRateCounter.increment(AMOUNT);
        final ResettingNonRateCounter otherResettingNonRateCounter = new ResettingNonRateCounter(monitorConfig);
        otherResettingNonRateCounter.increment(AMOUNT);
        assertEquals(otherResettingNonRateCounter.hashCode(), resettingNonRateCounter.hashCode());
        otherResettingNonRateCounter.increment();
        assertNotEquals(otherResettingNonRateCounter.hashCode(), resettingNonRateCounter.hashCode());
    }

    @Test
    public void testToString() {
        resettingNonRateCounter.increment(AMOUNT);
        final String expected = String.format("ResettingNonRateCounter{config=%s, count=%d}",
                monitorConfig.withAdditionalTag(COUNTER), AMOUNT);
        assertEquals(expected, resettingNonRateCounter.toString());
    }
}
