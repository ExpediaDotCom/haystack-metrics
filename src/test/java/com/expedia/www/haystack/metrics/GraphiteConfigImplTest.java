package com.expedia.www.haystack.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class GraphiteConfigImplTest {
    private static Random RANDOM = new Random();
    private static final String ADDRESS = RANDOM.nextLong() + "ADDRESS";
    private static final int PORT = RANDOM.nextInt();
    private static final int POLL_INTERVAL_SECONDS = RANDOM.nextInt();
    private static final int QUEUE_SIZE = RANDOM.nextInt();

    private GraphiteConfig graphiteConfig;

    @Before
    public void setUp() {
        graphiteConfig = new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE);
    }

    @Test
    public void testAddress() {
        assertEquals(ADDRESS, graphiteConfig.address());
    }

    @Test
    public void testPort() {
        assertEquals(PORT, graphiteConfig.port());
    }

    @Test
    public void testPollIntervalSeconds() {
        assertEquals(POLL_INTERVAL_SECONDS, graphiteConfig.pollintervalseconds());
    }

    @Test
    public void testQueueSize() {
        assertEquals(QUEUE_SIZE, graphiteConfig.queuesize());
    }

    @Test
    public void testEquals() {
        assertEquals(graphiteConfig, graphiteConfig);
        assertEquals(graphiteConfig, new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertEquals(new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE),
                new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE), graphiteConfig);
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl("", PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(ADDRESS, PORT + 1, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS + 1, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE + 1));
        assertNotEquals(graphiteConfig, null);
        assertNotEquals(graphiteConfig, "");
    }

    @Test
    public void testHashCode() {
        assertEquals(graphiteConfig.hashCode(), graphiteConfig.hashCode());
        assertEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(ADDRESS, PORT + 1, POLL_INTERVAL_SECONDS, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS + 1, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE + 1).hashCode());
    }
}
