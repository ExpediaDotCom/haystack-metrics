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

import com.netflix.servo.publish.AsyncMetricObserver;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.graphite.GraphiteMetricObserver;
import com.netflix.servo.util.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Publishes metrics to InfluxDb on a regular interval. The frequency of publishing is controlled by configuration.
 * Each application that uses this class must call MetricPublishing.start() in its main() method.
 */
@SuppressWarnings("WeakerAccess")
public class MetricPublishing {
    static final String ASYNC_METRIC_OBSERVER_NAME = "haystack";
    static final int POLL_INTERVAL_SECONDS_TO_EXPIRE_TIME_MULTIPLIER = 2000;
    static final String HOST_NAME_UNKNOWN_HOST_EXCEPTION = "HostName-UnknownHostException";
    static final String GRAPHITE_OBSERVER_DEBUG_MSG = "Creating GraphiteObserver with hostAndPort [%s] sendasrate [%b]";
    private static final String PREFIX = "${";
    private static final String SUFFIX = "}";

    private final Factory factory;
    private final Logger logger;
    private final PollScheduler pollScheduler = PollScheduler.getInstance();

    /**
     * Creates a new instance of MetricPublishing; intended to be used by non-unit-test code.
     */
    public MetricPublishing() {
        this(new Factory(), LoggerFactory.getLogger(MetricPublishing.class));
    }

    /**
     * Creates a new instance of MetricPublishing with a user-specified Factory; intended to be used by unit-test code
     * so that the Factory can be mocked.
     *
     * @param factory The factory to use.
     */
    MetricPublishing(Factory factory, Logger logger) {
        this.factory = factory;
        this.logger = logger;
    }

    /**
     * Starts the polling that will publish metrics at regular intervals. This start() method should be called by
     * the main() method of the application. Calling this method more than once is allowed, but will have no effect if
     * the polling has already been started.
     *
     * @param graphiteConfig Tells the library how to talk to Graphite
     */
    public void start(GraphiteConfig graphiteConfig) {
        synchronized (pollScheduler) {
            if(!pollScheduler.isStarted()) {
                pollScheduler.start();
                final MetricPoller monitorRegistryMetricPoller = factory.createMonitorRegistryMetricPoller();
                final List<MetricObserver> observers = Collections.singletonList(createGraphiteObserver(graphiteConfig));
                final PollRunnable task = factory.createTask(monitorRegistryMetricPoller, observers);
                pollScheduler.addPoller(task, graphiteConfig.pollintervalseconds(), SECONDS);
            }
        }
    }

    /**
     * Stops the polling that publishes metrics; for maximum safety, call this method before calling System.exit().
     */
    public void stop() {
        try {
            pollScheduler.stop();
        } catch(IllegalStateException e) {
            // The poller wasn't started; just ignore this error
        }
    }

    MetricObserver createGraphiteObserver(GraphiteConfig graphiteConfig) {
        final String host = getHost(graphiteConfig);
        final String hostAndPort = host + ":" + graphiteConfig.port();
        final MetricObserver graphiteMetricObserver = factory.createGraphiteMetricObserver(
                ASYNC_METRIC_OBSERVER_NAME, hostAndPort);
        final MetricObserver async = async(graphiteConfig, graphiteMetricObserver);
        final boolean sendasrate = graphiteConfig.sendasrate();
        final MetricObserver metricObserver = sendasrate ? rateTransform(graphiteConfig, async) : async;
        logger.info(String.format(GRAPHITE_OBSERVER_DEBUG_MSG, hostAndPort, sendasrate));
        return metricObserver;
    }

    @VisibleForTesting
    String getHost(GraphiteConfig graphiteConfig) {
        final String graphiteConfigHost = graphiteConfig.host();
        if(graphiteConfigHost.startsWith(PREFIX)) {
            final String environmentVariableName = graphiteConfigHost.substring(
                    PREFIX.length(), graphiteConfigHost.length() - SUFFIX.length());
            final Map<String, String> environmentVariables = factory.getEnvironmentVariables();
            return environmentVariables.get(environmentVariableName);
        }
        return graphiteConfigHost;
    }

    MetricObserver rateTransform(GraphiteConfig graphiteConfig, MetricObserver observer) {
        return factory.createCounterToRateMetricTransform(observer, graphiteConfig.pollintervalseconds(), SECONDS);
    }

    MetricObserver async(GraphiteConfig graphiteConfig, MetricObserver observer) {
        final long expireTime = POLL_INTERVAL_SECONDS_TO_EXPIRE_TIME_MULTIPLIER * graphiteConfig.pollintervalseconds();
        final int queueSize = graphiteConfig.queuesize();
        return factory.createAsyncMetricObserver(observer, queueSize, expireTime);
    }

    /**
     * Factory to wrap static or final methods; this Factory facilitates unit testing
     */
    static class Factory {
        Factory() {
            // default constructor
        }

        static String getLocalHostName(Factory factory) {
            try {
                return factory.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return HOST_NAME_UNKNOWN_HOST_EXCEPTION;
            }
        }

        MetricObserver createAsyncMetricObserver(MetricObserver observer, int queueSize, long expireTime) {
            return new AsyncMetricObserver(ASYNC_METRIC_OBSERVER_NAME, observer, queueSize, expireTime);
        }

        MetricObserver createCounterToRateMetricTransform(
                MetricObserver observer, long heartbeat, TimeUnit timeUnit) {
            return new CounterToRateMetricTransform(observer, heartbeat, timeUnit);
        }

        InetAddress getLocalHost() throws UnknownHostException {
            return InetAddress.getLocalHost();
        }

        MetricObserver createGraphiteMetricObserver(String prefix, String host) {
            final String hostName = Factory.getLocalHostName(this);
            return new GraphiteMetricObserver(prefix, host,
                    new ServoToInfluxDbViaGraphiteNamingConvention(hostName));
        }

        PollRunnable createTask(MetricPoller poller, Collection<MetricObserver> observers) {
            return new PollRunnable(poller, BasicMetricFilter.MATCH_ALL, true, observers);
        }

        MetricPoller createMonitorRegistryMetricPoller() {
            return new MonitorRegistryMetricPoller();
        }

        Map<String,String> getEnvironmentVariables() {
            return System.getenv();
        }
    }
}
