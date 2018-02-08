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

import com.expedia.www.haystack.metrics.MetricPublishing.Factory;
import com.netflix.servo.publish.AsyncMetricObserver;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.MetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.graphite.GraphiteMetricObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.expedia.www.haystack.metrics.MetricPublishing.ASYNC_METRIC_OBSERVER_NAME;
import static com.expedia.www.haystack.metrics.MetricPublishing.HOST_NAME_UNKNOWN_HOST_EXCEPTION;
import static com.expedia.www.haystack.metrics.MetricPublishing.POLL_INTERVAL_SECONDS_TO_EXPIRE_TIME_MULTIPLIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetricPublishingTest {
    private static final Random RANDOM = new Random();
    private static final int POLL_INTERVAL_SECONDS = RANDOM.nextInt(Short.MAX_VALUE);
    private static final int QUEUE_SIZE = RANDOM.nextInt(Byte.MAX_VALUE) + 1;
    private static final long EXPIRE_TIME = POLL_INTERVAL_SECONDS_TO_EXPIRE_TIME_MULTIPLIER * POLL_INTERVAL_SECONDS;
    private static final String HOST = String.format("%d.%d.%d.%d", RANDOM.nextInt(Byte.MAX_VALUE),
            RANDOM.nextInt(Byte.MAX_VALUE), RANDOM.nextInt(Byte.MAX_VALUE), RANDOM.nextInt(Byte.MAX_VALUE));
    private static final String PREFIX = RANDOM.nextLong() + "PREFIX";
    private static final int PORT = RANDOM.nextInt(Short.MAX_VALUE);
    private static final String HOST_AND_PORT = HOST + ':' + PORT;
    private static final int NUMBER_OF_ITERATIONS_IN_TESTS = RANDOM.nextInt(Byte.MAX_VALUE) + 2;

    @Mock
    private Factory mockFactory;

    @Mock
    private MetricObserver mockMetricObserver;

    @Mock
    private GraphiteConfig mockGraphiteConfig;

    @Mock
    private MetricObserver mockAsyncMetricObserver;

    @Mock
    private MetricObserver mockCounterToRateMetricTransform;

    @Mock
    private MetricObserver mockGraphiteMetricObserver;

    @Mock
    private PollRunnable mockTask;

    @Mock
    private MetricPoller mockMetricPoller;

    // Objects under test
    private MetricPublishing metricPublishing;
    private Factory factory;

    @Before
    public void setUp() {
        metricPublishing = new MetricPublishing(mockFactory);
        factory = new Factory();
    }

    @After
    public void tearDown() {
        if(PollScheduler.getInstance().isStarted()) {
            PollScheduler.getInstance().stop();
        }
        verifyNoMoreInteractions(mockFactory, mockMetricObserver, mockGraphiteConfig, mockAsyncMetricObserver,
                mockCounterToRateMetricTransform, mockGraphiteMetricObserver, mockTask, mockMetricPoller);
    }

    @Test
    public void testStart() throws InterruptedException {
        final List<MetricObserver> observers = whensForStart();
        when(mockGraphiteConfig.sendasrate()).thenReturn(true);

        for(int i = 0 ; i < NUMBER_OF_ITERATIONS_IN_TESTS ; i++) {
            metricPublishing.start(mockGraphiteConfig);
        }

        // Would mock PollScheduler, but it's final; instead, sleep to give mockTask.run() time to be called
        Thread.sleep(1000);
        verify(mockGraphiteConfig).sendasrate();
        verifiesForStart(observers);
        metricPublishing.stop();
    }

    @Test(expected = OutOfMemoryError.class)
    public void testStartThrowsException() {
        final OutOfMemoryError outOfMemoryError = new OutOfMemoryError("Test");
        when(mockFactory.createMonitorRegistryMetricPoller()).thenThrow(outOfMemoryError);
        try {
            metricPublishing.start(mockGraphiteConfig);
        } catch(OutOfMemoryError e) {
            assertSame(outOfMemoryError, e);
            verify(mockFactory).createMonitorRegistryMetricPoller();
            throw e;
        }
    }

    private List<MetricObserver> whensForStart() {
        whensForCreateGraphiteObserver();
        when(mockFactory.createMonitorRegistryMetricPoller()).thenReturn(mockMetricPoller);
        when(mockFactory.createTask(any(MetricPoller.class), anyListOf(MetricObserver.class))).thenReturn(mockTask);
        return Collections.singletonList(mockCounterToRateMetricTransform);
    }

    private void verifiesForStart(List<MetricObserver> observers) {
        verifiesForAsync(3, mockGraphiteMetricObserver);
        verify(mockFactory).createCounterToRateMetricTransform(mockAsyncMetricObserver, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        verifiesForCreateGraphiteObserver(3);
        verify(mockFactory).createMonitorRegistryMetricPoller();
        verify(mockFactory).createTask(mockMetricPoller, observers);
        verify(mockTask).run();
    }

    @Test
    public void testCreateGraphiteObserver() {
        whensForCreateGraphiteObserver();

        final MetricObserver metricObserver = metricPublishing.createGraphiteObserver(mockGraphiteConfig);
        assertSame(mockAsyncMetricObserver, metricObserver);

        verify(mockGraphiteConfig).sendasrate();
        verifiesForAsync(1, mockGraphiteMetricObserver);
        verifiesForCreateGraphiteObserver(1);
    }

    private void whensForCreateGraphiteObserver() {
        whensForAsync();
        whensForRateTransform();
        when(mockGraphiteConfig.host()).thenReturn(HOST);
        when(mockGraphiteConfig.port()).thenReturn(PORT);
        when(mockFactory.createGraphiteMetricObserver(anyString(), anyString())).thenReturn(mockGraphiteMetricObserver);
    }

    private void verifiesForCreateGraphiteObserver(int wantedNumberOfInvocations) {
        verify(mockGraphiteConfig, times(wantedNumberOfInvocations)).pollintervalseconds();
        verify(mockGraphiteConfig).host();
        verify(mockGraphiteConfig).port();
        verify(mockFactory).createGraphiteMetricObserver(ASYNC_METRIC_OBSERVER_NAME, HOST_AND_PORT);
    }

    @Test
    public void testRateTransform() {
        whensForRateTransform();

        final MetricObserver metricObserver = metricPublishing.rateTransform(mockGraphiteConfig, mockMetricObserver);
        assertSame(mockCounterToRateMetricTransform, metricObserver);

        verify(mockFactory).createCounterToRateMetricTransform(mockMetricObserver, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        verify(mockGraphiteConfig, times(1)).pollintervalseconds();
    }

    private void whensForRateTransform() {
        when(mockFactory.createCounterToRateMetricTransform(any(MetricObserver.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(mockCounterToRateMetricTransform);
        when(mockGraphiteConfig.pollintervalseconds()).thenReturn(POLL_INTERVAL_SECONDS);
    }

    @Test
    public void testStopWithoutCallingStartFirst() {
        metricPublishing.stop();
        // PollScheduler is a final class and cannot be mocked with Mockito, so traditional verification of
        // metricPublishing.stop() cannot be done. Instead, don't call start(), and catch the resulting exception
        // that will only occur when stop() is called without calling start(). This exception is caught and ignored
        // by MetricPublishing.stop(). Since it is silently ignored, only code coverage verifies the ignoring behavior.
    }

    @Test
    public void testAsync() {
        whensForAsync();

        final MetricObserver metricObserver = metricPublishing.async(mockGraphiteConfig, mockMetricObserver);
        assertSame(mockAsyncMetricObserver, metricObserver);

        verifiesForAsync(1, mockMetricObserver);
    }

    private void whensForAsync() {
        when(mockGraphiteConfig.pollintervalseconds()).thenReturn(POLL_INTERVAL_SECONDS);
        when(mockGraphiteConfig.queuesize()).thenReturn(QUEUE_SIZE);
        when(mockFactory.createAsyncMetricObserver(any(MetricObserver.class), anyInt(), anyLong()))
                .thenReturn(mockAsyncMetricObserver);
    }

    private void verifiesForAsync(int pollIntervalSecondsTimes, MetricObserver metricObserver) {
        verify(mockGraphiteConfig, times(pollIntervalSecondsTimes)).pollintervalseconds();
        verify(mockGraphiteConfig).queuesize();
        verify(mockFactory).createAsyncMetricObserver(
                metricObserver, QUEUE_SIZE, EXPIRE_TIME);
    }

    @Test
    public void testFactoryCreateAsyncMetricObserver() {
        final MetricObserver metricObserver = factory.createAsyncMetricObserver(
                mockMetricObserver, QUEUE_SIZE, EXPIRE_TIME);
        assertEquals(ASYNC_METRIC_OBSERVER_NAME, metricObserver.getName());
        assertEquals(AsyncMetricObserver.class, metricObserver.getClass());
    }

    @Test
    public void testFactoryCreateCounterToRateMetricTransform() {
        when(mockMetricObserver.getName()).thenReturn(ASYNC_METRIC_OBSERVER_NAME);

        final MetricObserver metricObserver = factory.createCounterToRateMetricTransform(
                mockMetricObserver, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);

        assertEquals(ASYNC_METRIC_OBSERVER_NAME, metricObserver.getName());
        assertEquals(CounterToRateMetricTransform.class, metricObserver.getClass());
        verify(mockMetricObserver).getName();
    }

    @Test
    public void testFactoryCreateGraphiteMetricObserver() {
        final MetricObserver metricObserver = factory.createGraphiteMetricObserver(PREFIX, HOST_AND_PORT);
        assertEquals("GraphiteMetricObserver" + PREFIX, metricObserver.getName());
        assertEquals(GraphiteMetricObserver.class, metricObserver.getClass());
    }

    @Test
    public void testFactoryCreateTask() {
        when(mockMetricPoller.poll(any(MetricFilter.class), anyBoolean())).thenReturn(Collections.emptyList());

        final PollRunnable task = factory.createTask(mockMetricPoller, Collections.emptyList());
        task.run();

        verify(mockMetricPoller).poll(BasicMetricFilter.MATCH_ALL, true);
    }

    @Test
    public void testFactoryCreateMonitorRegistryMetricPoller() {
        when(mockMetricPoller.poll(any(MetricFilter.class), anyBoolean())).thenReturn(Collections.emptyList());

        final MetricPoller metricPoller = factory.createMonitorRegistryMetricPoller();

        assertEquals(MonitorRegistryMetricPoller.class, metricPoller.getClass());
    }

    @Test
    public void testDefaultConstructor() {
        new MetricPublishing();
    }

    @Test
    public void testFactoryGetLocalHostUnknownHostException() throws UnknownHostException {
        when(mockFactory.getLocalHost()).thenThrow(new UnknownHostException());

        final String localHostName = Factory.getLocalHostName(mockFactory);

        assertEquals(HOST_NAME_UNKNOWN_HOST_EXCEPTION, localHostName);
        verify(mockFactory).getLocalHost();
    }
}
