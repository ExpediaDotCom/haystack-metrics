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

public class ResettingCounterTest {
    private static final Random RANDOM = new Random();
    private static final String COUNTER_NAME = RANDOM.nextLong() + "COUNTER_NAME";
    private static final int POLLER_INDEX = RANDOM.nextInt();
    private static final long AMOUNT = RANDOM.nextLong();

    private MonitorConfig monitorConfig;
    private ResettingCounter resettingCounter;

    @Before
    public void setUp() {
        monitorConfig = MonitorConfig.builder(COUNTER_NAME).build();
        resettingCounter = new ResettingCounter(monitorConfig);
    }

    @Test
    public void testConstructor() {
        final TagList tags = resettingCounter.getConfig().getTags();
        assertEquals(1, tags.size());
        for (Tag tag : tags) {
            assertEquals(DataSourceType.KEY, tag.getKey());
            assertEquals(COUNTER.toString(), tag.getValue());
        }
    }

    @Test
    public void testIncrement() {
        resettingCounter.increment();

        assertEquals(1L, resettingCounter.getValue(POLLER_INDEX));
    }

    @Test
    public void testIncrementWithValue() {
        resettingCounter.increment(AMOUNT);

        assertEquals(AMOUNT, resettingCounter.getValue(POLLER_INDEX));
    }

    @Test
    public void testGetValue() {
        resettingCounter.increment(AMOUNT);

        resettingCounter.getValue(POLLER_INDEX);
        assertEquals(0L, resettingCounter.getValue(POLLER_INDEX));
    }

    @Test
    public void testEquals() {
        assertEquals(resettingCounter, resettingCounter);
        assertNotEquals(resettingCounter, null);
        assertNotEquals(resettingCounter, "Object that is not an instance of ResettingCounter");
        final ResettingCounter otherResettingCounter = new ResettingCounter(monitorConfig);
        assertEquals(resettingCounter, otherResettingCounter);
        otherResettingCounter.increment();
        assertNotEquals(resettingCounter, otherResettingCounter);
        assertNotEquals(resettingCounter, new ResettingCounter(MonitorConfig.builder("").build()));
    }

    @Test
    public void testHashCode() {
        resettingCounter.increment(AMOUNT);
        final ResettingCounter otherResettingCounter = new ResettingCounter(monitorConfig);
        otherResettingCounter.increment(AMOUNT);
        assertEquals(otherResettingCounter.hashCode(), resettingCounter.hashCode());
        otherResettingCounter.increment();
        assertNotEquals(otherResettingCounter.hashCode(), resettingCounter.hashCode());
    }

    @Test
    public void testToString() {
        resettingCounter.increment(AMOUNT);
        final String expected = String.format("ResettingCounter{config=%s, count=%d}",
                monitorConfig.withAdditionalTag(COUNTER), AMOUNT);
        assertEquals(expected, resettingCounter.toString());
    }
}
