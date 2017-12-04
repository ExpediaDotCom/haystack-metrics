/*
 * Copyright 2017 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class GraphiteConfigImplTest {
    private static Random RANDOM = new Random();
    private static final String HOST = RANDOM.nextLong() + "HOST";
    private static final int PORT = RANDOM.nextInt();
    private static final int POLL_INTERVAL_SECONDS = RANDOM.nextInt();
    private static final int QUEUE_SIZE = RANDOM.nextInt();

    private GraphiteConfig graphiteConfig;

    @Before
    public void setUp() {
        graphiteConfig = new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE);
    }

    @Test
    public void testHost() {
        assertEquals(HOST, graphiteConfig.host());
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
        assertEquals(graphiteConfig, new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertEquals(new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE),
                new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE), graphiteConfig);
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl("", PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(HOST, PORT + 1, POLL_INTERVAL_SECONDS, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS + 1, QUEUE_SIZE));
        assertNotEquals(graphiteConfig, new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE + 1));
        assertNotEquals(graphiteConfig, null);
        assertNotEquals(graphiteConfig, "");
    }

    @Test
    public void testHashCode() {
        assertEquals(graphiteConfig.hashCode(), graphiteConfig.hashCode());
        assertEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(null, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(HOST, PORT + 1, POLL_INTERVAL_SECONDS, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS + 1, QUEUE_SIZE).hashCode());
        assertNotEquals(graphiteConfig.hashCode(), new GraphiteConfigImpl(HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE + 1).hashCode());
    }
}
