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

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.TagList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Random;

import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_CLASS;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_FULLY_QUALIFIED_CLASS_NAME;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_LINE_NUMBER;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_METRIC_GROUP;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_SUBSYSTEM;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetricObjectsTest {
    private final static Random RANDOM = new Random();
    private final static String METRIC_GROUP = RANDOM.nextLong() + "METRIC_GROUP";
    private final static String SUBSYSTEM = RANDOM.nextLong() + "SUBSYSTEM";
    private final static String APPLICATION = RANDOM.nextLong() + "APPLICATION";
    private final static String FULLY_QUALIFIED_CLASS_NAME = RANDOM.nextLong() + "FULLY_QUALIFIED_CLASS_NAME";
    private final static String CLASS = RANDOM.nextLong() + "CLASS";
    private final static String LINE_NUMBER = Integer.toString(RANDOM.nextInt(Integer.MAX_VALUE));
    private final static String METRIC_NAME = RANDOM.nextLong() + "METRIC_NAME";

    @Mock
    private MetricObjects.Factory mockFactory;

    @Mock
    private MonitorRegistry mockMonitorRegistry;

    @Mock
    private Logger mockLogger;

    // Objects under test
    private MetricObjects metricObjects;
    private MetricObjects.Factory factory;

    @Before
    public void setUp() {
        metricObjects = new MetricObjects(mockFactory, mockLogger);
        factory = new MetricObjects.Factory();
    }

    @After
    public void tearDown() {
        MetricObjects.BASIC_COUNTERS.clear();
        MetricObjects.RESETTING_NON_RATE_COUNTERS.clear();
        MetricObjects.TIMERS.clear();
        verifyNoMoreInteractions(mockFactory, mockMonitorRegistry, mockLogger);
    }

    @Test
    public void testCreateAndRegisterCounter() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Counter counter = metricObjects.createAndRegisterCounter(SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);

        assertsAndVerifiesForCreateAndRegisterCounter(counter);
    }

    @Test
    public void testCreateAndRegisterExistingCounter() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Counter counter = metricObjects.createAndRegisterCounter(SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);
        final Counter existingCounter = metricObjects.createAndRegisterCounter(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);

        assertSame(counter, existingCounter);
        verify(mockLogger).warn(String.format(MetricObjects.COUNTER_ALREADY_REGISTERED, existingCounter));
        assertsAndVerifiesForCreateAndRegisterCounter(counter);
    }

    private void assertsAndVerifiesForCreateAndRegisterCounter(Counter counter) {
        assertsAndVerifiesForCreateAndRegister(counter, 4);
        final TagList tagList = counter.getConfig().getTags();
        assertEquals(DataSourceType.COUNTER.getValue(), tagList.getValue(DataSourceType.KEY));
        assertTrue(counter instanceof BasicCounter);
    }

    @Test
    public void testCreateAndRegisterResettingCounter() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Counter counter = metricObjects.createAndRegisterResettingCounter(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);

        assertsAndVerifiesForCreateAndRegisterResettingCounter(counter);
    }

    @Test
    public void testCreateAndRegisterExistingResettingCounter() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Counter counter = metricObjects.createAndRegisterResettingCounter(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);
        final Counter existingCounter = metricObjects.createAndRegisterResettingCounter(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);

        assertSame(counter, existingCounter);
        verify(mockLogger).warn(String.format(MetricObjects.COUNTER_ALREADY_REGISTERED, existingCounter));
        assertsAndVerifiesForCreateAndRegisterResettingCounter(counter);
    }

    private void assertsAndVerifiesForCreateAndRegisterResettingCounter(Counter counter) {
        assertsAndVerifiesForCreateAndRegister(counter, 4);
        final TagList tagList = counter.getConfig().getTags();
        assertEquals(DataSourceType.COUNTER.getValue(), tagList.getValue(DataSourceType.KEY));
        assertTrue(counter instanceof ResettingCounter);
    }

    @Test
    public void testCreateAndRegisterResettingCounterErrorType() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Counter counter = metricObjects.createAndRegisterResettingCounter(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME);

        assertsAndVerifiesForCreateAndRegisterResettingCounter(counter);
    }

    @Test
    public void testCreateAndRegisterExistingResettingCounterErrorType() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Counter counter = metricObjects.createAndRegisterResettingCounter(
                METRIC_GROUP, SUBSYSTEM, FULLY_QUALIFIED_CLASS_NAME, LINE_NUMBER, METRIC_NAME);
        final Counter existingCounter = metricObjects.createAndRegisterResettingCounter(
                METRIC_GROUP, SUBSYSTEM, FULLY_QUALIFIED_CLASS_NAME, LINE_NUMBER, METRIC_NAME);

        assertSame(counter, existingCounter);
        verify(mockLogger).warn(String.format(MetricObjects.COUNTER_ALREADY_REGISTERED, existingCounter));
        assertsAndVerifiesForCreateAndRegisterResettingCounterErrorType(counter);
    }

    private void assertsAndVerifiesForCreateAndRegisterResettingCounterErrorType(Counter counter) {
        assertsAndVerifiesForCreateAndRegisterErrorType(counter);
        final TagList tagList = counter.getConfig().getTags();
        assertEquals(DataSourceType.COUNTER.getValue(), tagList.getValue(DataSourceType.KEY));
        assertTrue(counter instanceof ResettingCounter);
    }

    @Test
    public void testCreateAndRegisterBasicTimer() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Timer timer = metricObjects.createAndRegisterBasicTimer(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME, MILLISECONDS);

        assertsAndVerifiesForCreateAndRegister(timer, 3);
    }

    @Test
    public void testCreateAndRegisterExistingBasicTimer() {
        when(mockFactory.getMonitorRegistry()).thenReturn(mockMonitorRegistry);

        final Timer timer = metricObjects.createAndRegisterBasicTimer(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME, MILLISECONDS);
        final Timer existingTimer = metricObjects.createAndRegisterBasicTimer(
                SUBSYSTEM, APPLICATION, CLASS, METRIC_NAME, MILLISECONDS);

        assertSame(timer, existingTimer);
        verify(mockLogger).warn(String.format(MetricObjects.TIMER_ALREADY_REGISTERED, existingTimer));
        assertsAndVerifiesForCreateAndRegister(timer, 3);
    }

    private void assertsAndVerifiesForCreateAndRegister(Monitor<?> monitor, int expectedTagListSize) {
        final TagList tagList = monitor.getConfig().getTags();
        assertEquals(expectedTagListSize, tagList.size());
        assertEquals(SUBSYSTEM, tagList.getValue(TAG_KEY_SUBSYSTEM));
        assertEquals(CLASS, tagList.getValue(TAG_KEY_CLASS));
        assertEquals(METRIC_NAME, monitor.getConfig().getName());
        verify(mockMonitorRegistry).register(monitor);
        verify(mockFactory).getMonitorRegistry();
    }

    private void assertsAndVerifiesForCreateAndRegisterErrorType(Monitor<?> monitor) {
        final TagList tagList = monitor.getConfig().getTags();
        assertEquals(5, tagList.size());
        assertEquals(METRIC_GROUP, tagList.getValue(TAG_KEY_METRIC_GROUP));
        assertEquals(SUBSYSTEM, tagList.getValue(TAG_KEY_SUBSYSTEM));
        assertEquals(FULLY_QUALIFIED_CLASS_NAME, tagList.getValue(TAG_KEY_FULLY_QUALIFIED_CLASS_NAME));
        assertEquals(LINE_NUMBER, tagList.getValue(TAG_KEY_LINE_NUMBER));
        assertEquals(METRIC_NAME, monitor.getConfig().getName());
        verify(mockMonitorRegistry).register(monitor);
        verify(mockFactory).getMonitorRegistry();
    }

    @Test
    public void testFactoryGetDefaultMonitorRegisterInstance() {
        assertSame(DefaultMonitorRegistry.getInstance(), factory.getMonitorRegistry());
    }

    @Test
    public void testDefaultConstructor() {
        new MetricObjects();
    }
}
