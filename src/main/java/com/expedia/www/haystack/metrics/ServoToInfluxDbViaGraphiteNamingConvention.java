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

import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.publish.graphite.GraphiteNamingConvention;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_APPLICATION;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_CLASS;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_SUBSYSTEM;

/**
 * Facilitates the creation of a Graphite plain text message that conforms to the Haystack standard, so that such a
 * Graphite message can be produced by Servo and consumed by InfluxDb, creating tags from the period-delimited pieces of
 * the Graphite message.
 */
public class ServoToInfluxDbViaGraphiteNamingConvention implements GraphiteNamingConvention {
    static final String MISSING_TAG = "MISSING_TAG_%s";
    static final String METRIC_FORMAT = "%s.%s.%s.%s.%s_%s";
    static final String STATISTIC_TAG_NAME = "statistic";

    private final String hostName;

    ServoToInfluxDbViaGraphiteNamingConvention(String hostName) {
        this.hostName = cleanup(hostName);
    }

    /**
     * Creates the name of the metric to be passed to Graphite.
     *
     * @param metric the metric
     * @return a String with five fields and four periods: subsystem.application.hostName.class.NAME_TYPE where:
     * <ul>
     * <li>subsystem is the user-defined module name, probably something like "trends" or "pipes".</li>
     * <li>application is the user-defined application name (applications are part of the subsystem).</li>
     * <li>hostName is the name of the host, obtained programmatically by calling
     * <code>InetAddress.getLocalHost().getHostName().</code></li>
     * <li>class is often the simple name of the Java/Scala class/object that contains the Counter/Timer etc.,
     * but can be hard-coded value that will not change if the class/object code is refactored.</li>
     * <li>NAME is the name of the Counter, Timer etc. (upper case suggested so as to match TYPE below).</li>
     * <li>TYPE is the metric name assigned by Servo (for example, a Counter emits a metric name of RATE).</li>
     * </ul>
     * Before the metric is sent to graphite, one additional field will be added as a prefix: a period-delimited
     * "system" name whose value is the value of the haystack.graphite.prefix configuration. Typically this value is
     * "haystack" and its value must be part of the InfluxDb filter that changes Graphite-style metrics into InfluxDb
     * tagged metrics.
     */
    @Override
    public String getName(Metric metric) {
        final MonitorConfig config = metric.getConfig();
        final TagList tags = config.getTags();
        final String subsystem = cleanup(tags, TAG_KEY_SUBSYSTEM);
        final String application = cleanup(tags, TAG_KEY_APPLICATION);
        final String klass = cleanup(tags, TAG_KEY_CLASS);
        final String configName = config.getName(); // Servo disallows null for name, no need to cleanup
        final String type = cleanup(tags, DataSourceType.KEY);

        // Timer comes with a statistic tag; Counter does not
        final Tag statisticTag = tags.getTag(STATISTIC_TAG_NAME);
        final String statisticName = statisticTag == null ? null : statisticTag.getValue();
        final String metricName = statisticName == null ? type : type + '_' + statisticName;
        return String.format(METRIC_FORMAT, subsystem, application, hostName, klass, configName, metricName);
    }

    private static String cleanup(TagList tags, String name) {
        return cleanup(tags.getTag(name), name);
    }

    private static String cleanup(Tag tag, String name) {
        if (tag == null) {
            return String.format(MISSING_TAG, name);
        }
        return cleanup(tag.getValue());
    }

    private static String cleanup(String value) {
        // Servo disallows null or "" in tag value, so there's no need to check for that here;
        // just handle spaces and periods, replacing each such character with an underscore.
        return value.replace(" ", "_").replace(".", "_");
    }
}
