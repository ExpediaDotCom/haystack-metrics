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
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.BucketConfig;
import com.netflix.servo.monitor.BucketTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.SmallTagMap;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.TaggingContext;
import com.netflix.servo.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Creates Servo's Counter and Timer objects, registering them with the default monitor registry when creating them.
 */
@SuppressWarnings("WeakerAccess")
public class MetricObjects {
    static final String TAG_KEY_METRIC_GROUP = "metricGroup";
    static final String TAG_KEY_SUBSYSTEM = "subsystem";
    static final String TAG_KEY_APPLICATION = "application";
    static final String TAG_KEY_FULLY_QUALIFIED_CLASS_NAME = "fullyQualifiedClassName";
    static final String TAG_KEY_CLASS = "class";
    static final String TAG_KEY_LINE_NUMBER = "lineNumber";
    static final String COUNTER_ALREADY_REGISTERED = "The Counter %s has already been registered";
    static final String TIMER_ALREADY_REGISTERED = "The Timer %s has already been registered";
    static final String METRIC_GROUP_BUCKETS = "buckets";
    static final ConcurrentMap<MonitorConfig, Counter> BASIC_COUNTERS = new ConcurrentHashMap<>();
    static final ConcurrentMap<MonitorConfig, Counter> RESETTING_NON_RATE_COUNTERS = new ConcurrentHashMap<>();
    static final ConcurrentMap<MonitorConfig, Timer> TIMERS = new ConcurrentHashMap<>();

    private final Factory factory;
    private final Logger logger;

    /**
     * Create a new instance of MetricObjects; intended to be used by non-unit-test code.
     */
    public MetricObjects() {
        this(new Factory(), LoggerFactory.getLogger(MetricObjects.class));
    }

    /**
     * Create a new instance of MetricObjects with a user-specified Factory; intended to be used by unit-test code
     * so that the Factory can be mocked.
     *
     * @param factory The Factory to use to obtain a MonitorRegistry
     */
    MetricObjects(Factory factory, Logger logger) {
        this.logger = logger;
        this.factory = factory;
    }

    /**
     * Creates a new BasicCounter; you should only call this method once for each Counter in your code. This method is
     * thread-safe, because both of Servo's implementations of the MonitorRegistry interface use thread-safe
     * implementations to hold the registered monitors: com.netflix.servo.jmx.JmxMonitorRegistry uses a ConcurrentMap,
     * and com.netflix.servo.BasicMonitorRegistry uses Collections.synchronizedSet().
     * If you call the method twice with the same arguments, the Counter created during the first call will be returned
     * by the second call.
     *
     * @param subsystem   the subsystem, typically something like "pipes" or "trends".
     * @param application the application in the subsystem.
     * @param klass       the metric class, frequently (but not necessarily) the class containing the Counter.
     * @param counterName the name of the Counter, usually the name of the variable holding the Counter instance;
     *                    using upper case for counterName is recommended.
     * @return a new Counter that this method registers in the DefaultMonitorRegistry before returning it.
     */
    public Counter createAndRegisterCounter(String subsystem, String application, String klass, String counterName) {
        final MonitorConfig monitorConfig = buildMonitorConfig(subsystem, application, klass, counterName);
        return checkForExistingCounter(
                monitorConfig, new BasicCounter(monitorConfig), BASIC_COUNTERS);
    }

    /**
     * Creates a new ResettingCounter with three tags; you should only call this method once for each Counter in your
     * code. This method is thread-safe, because both of Servo's implementations of the MonitorRegistry interface use
     * thread-safe implementations to hold the registered monitors: com.netflix.servo.jmx.JmxMonitorRegistry uses a
     * ConcurrentMap, and com.netflix.servo.BasicMonitorRegistry uses Collections.synchronizedSet(). If you call the
     * method twice with the same arguments, the Counter created during the first call will be returned by the second
     * call.
     *
     * @param subsystem   the subsystem, typically something like "pipes" or "trends".
     * @param application the application in the subsystem.
     * @param klass       the metric class, frequently (but not necessarily) the class containing the Counter.
     * @param counterName the name of the Counter, usually the name of the variable holding the Counter instance;
     *                    using upper case for counterName is recommended.
     * @return a new Counter that this method registers in the DefaultMonitorRegistry before returning it.
     */
    public Counter createAndRegisterResettingCounter(String subsystem, String application, String klass, String counterName) {
        final MonitorConfig monitorConfig = buildMonitorConfig(subsystem, application, klass, counterName);
        return checkForExistingCounter(
                monitorConfig, new ResettingCounter(monitorConfig), RESETTING_NON_RATE_COUNTERS);
    }

    /**
     * Creates a new ResettingCounter with four tags; you should only call this method once for each Counter in your
     * code. This method is thread-safe, because both of Servo's implementations of the MonitorRegistry interface use
     * thread-safe implementations to hold the registered monitors: com.netflix.servo.jmx.JmxMonitorRegistry uses a
     * ConcurrentMap, and com.netflix.servo.BasicMonitorRegistry uses Collections.synchronizedSet(). If you call the
     * method twice with the same arguments, the Counter created during the first call will be returned by the second
     * call.
     *
     * @param metricGroup             the metric group, typically "errors" (the first use case for which this API was
     *                                written).
     * @param subsystem               the subsystem, typically something like "pipes" or "trends".
     * @param fullyQualifiedClassName the fully (package) qualified class name, with '.' replaced by '-'.
     * @param lineNumber              the line number of the source code at which the error occurred or was logged
     * @param counterName             the name of the Counter, usually the name of the variable holding the Counter
     *                                instance; using upper case for counterName is recommended.
     * @return a new Counter that this method registers in the DefaultMonitorRegistry before returning it.
     */
    public Counter createAndRegisterResettingCounter(String metricGroup,
                                                     String subsystem,
                                                     String fullyQualifiedClassName,
                                                     String lineNumber,
                                                     String counterName) {
        final MonitorConfig monitorConfig = buildMonitorConfigForErrors(
                metricGroup, subsystem, fullyQualifiedClassName, lineNumber, counterName);
        return checkForExistingCounter(
                monitorConfig, new ResettingCounter(monitorConfig), RESETTING_NON_RATE_COUNTERS);
    }

    private Counter checkForExistingCounter(MonitorConfig monitorConfig,
                                            Counter counter,
                                            Map<MonitorConfig, Counter> counters) {
        final Counter existingCounter = counters.putIfAbsent(monitorConfig, counter);
        if (existingCounter != null) {
            logger.warn(String.format(COUNTER_ALREADY_REGISTERED, existingCounter.toString()));
            return existingCounter;
        }
        factory.getMonitorRegistry().register(counter);
        return counter;
    }

    /**
     * Creates a new BasicTimer; you should only call this method once for each BasicTimer in your code.
     * This method is thread-safe; see the comments in {@link #createAndRegisterCounter}.
     * If you call the method twice with the same arguments, the Timer created during the first call will be returned
     * by the second call. Note that the Timer configuration specified by the first four arguments to this method must
     * be unique across all Timers (BasicTimer, BucketTimer, and StatsTimer).
     *
     * @param subsystem   the subsystem, typically something like "pipes" or "trends".
     * @param application the application in the subsystem.
     * @param klass       the metric class, frequently (but not necessarily) the class containing the Timer.
     * @param timerName   the name of the Timer, usually the name of the variable holding the Timer instance;
     *                    using upper case for timerName is recommended.
     * @param timeUnit    desired precision, typically TimeUnit.MILLISECONDS.
     * @return a new BasicTimer that this method registers in the DefaultMonitorRegistry before returning it.
     */
    public Timer createAndRegisterBasicTimer(
            String subsystem, String application, String klass, String timerName, TimeUnit timeUnit) {
        final MonitorConfig monitorConfig = buildMonitorConfig(subsystem, application, klass, timerName);
        return checkForExistingTimer(monitorConfig, new BasicTimer(monitorConfig, timeUnit));
    }

    /**
     * Creates a new BucketTimer; you should only call this method once for each BucketTimer in your code.
     * This method is thread-safe; see the comments in {@link #createAndRegisterCounter}.
     * If you call the method twice with the same arguments, the Timer created during the first call will be returned
     * by the second call. Note that the Timer configuration specified by the second through fifth arguments to this
     * method must be unique across all Timers (BasicTimer, BucketTimer, and StatsTimer).
     *
     * @param subsystem   the subsystem, typically something like "pipes" or "trends".
     * @param application the application in the subsystem.
     * @param timerName   the name of the Timer, usually the name of the variable holding the Timer instance;
     *                    using upper case for timerName is recommended.
     * @param timeUnit    desired precision, typically TimeUnit.MILLISECONDS.
     * @param buckets     the buckets to be used; @see BucketConfig#withBuckets
     * @return a new BucketTimer that this method registers in the DefaultMonitorRegistry before returning it.
     */
    public Timer createAndRegisterBucketTimer(String subsystem,
                                              String application,
                                              String timerName,
                                              TimeUnit timeUnit,
                                              long... buckets) {
        final MonitorConfig monitorConfig = buildMonitorConfigForBuckets(
                subsystem, application, timerName);
        final BucketConfig bucketConfig = new BucketConfig.Builder().withBuckets(buckets).build();
        return checkForExistingTimer(monitorConfig, new BucketTimer(monitorConfig, bucketConfig, timeUnit));
    }

    private Timer checkForExistingTimer(MonitorConfig monitorConfig, Timer timer) {
        final Timer existingTimer = TIMERS.putIfAbsent(monitorConfig, timer);
        if (existingTimer != null) {
            logger.warn(String.format(TIMER_ALREADY_REGISTERED, existingTimer.toString()));
            return existingTimer;
        }
        factory.getMonitorRegistry().register(timer);
        return timer;
    }

    private MonitorConfig buildMonitorConfigForErrors(String metricGroup,
                                                      String subsystem,
                                                      String fullyQualifiedClassName,
                                                      String lineNumber,
                                                      String monitorName) {
        final TaggingContext taggingContext = () -> getTagsForErrors(
                metricGroup, subsystem, fullyQualifiedClassName, lineNumber);
        return MonitorConfig.builder(monitorName).withTags(taggingContext.getTags()).build();
    }

    private MonitorConfig buildMonitorConfigForBuckets(String subsystem, String application, String monitorName) {
        final TaggingContext taggingContext = () -> getTagsForBuckets(subsystem, application);
        return MonitorConfig.builder(monitorName).withTags(taggingContext.getTags()).build();
    }

    private MonitorConfig buildMonitorConfig(String subsystem, String application, String klass, String monitorName) {
        final TaggingContext taggingContext = () -> getTags(subsystem, application, klass);
        return MonitorConfig.builder(monitorName).withTags(taggingContext.getTags()).build();
    }

    private static TagList getTagsForErrors(
            String metricGroup, String subsystem, String fullyQualifiedClassName, String lineNumber) {
        final SmallTagMap.Builder builder = new SmallTagMap.Builder(4);
        builder.add(Tags.newTag(TAG_KEY_METRIC_GROUP, metricGroup));
        builder.add(Tags.newTag(TAG_KEY_LINE_NUMBER, lineNumber));
        builder.add(Tags.newTag(TAG_KEY_SUBSYSTEM, subsystem));
        builder.add(Tags.newTag(TAG_KEY_FULLY_QUALIFIED_CLASS_NAME, fullyQualifiedClassName));
        return new BasicTagList(builder.result());
    }

    @SuppressWarnings("Duplicates")
    private static TagList getTagsForBuckets(String subsystem, String application) {
        final SmallTagMap.Builder builder = new SmallTagMap.Builder(3);
        builder.add(Tags.newTag(TAG_KEY_METRIC_GROUP, METRIC_GROUP_BUCKETS));
        builder.add(Tags.newTag(TAG_KEY_SUBSYSTEM, subsystem));
        builder.add(Tags.newTag(TAG_KEY_APPLICATION, application));
        return new BasicTagList(builder.result());
    }

    @SuppressWarnings("Duplicates")
    private static TagList getTags(String subsystem, String application, String klass) {
        final SmallTagMap.Builder builder = new SmallTagMap.Builder(3);
        builder.add(Tags.newTag(TAG_KEY_SUBSYSTEM, subsystem));
        builder.add(Tags.newTag(TAG_KEY_APPLICATION, application));
        builder.add(Tags.newTag(TAG_KEY_CLASS, klass));
        return new BasicTagList(builder.result());
    }

    /**
     * Factory to wrap static or final methods; this Factory facilitates unit testing.
     */
    static class Factory {
        Factory() {
            // default constructor
        }

        MonitorRegistry getMonitorRegistry() {
            return DefaultMonitorRegistry.getInstance();
        }
    }
}

