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
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.expedia.www.haystack.metrics.MetricObjects.METRIC_GROUP_BUCKETS;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_APPLICATION;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_FULLY_QUALIFIED_CLASS_NAME;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_LINE_NUMBER;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_METRIC_GROUP;
import static com.expedia.www.haystack.metrics.ServoToInfluxDbViaGraphiteNamingConvention.METRIC_FORMAT_6_ARGS;
import static com.expedia.www.haystack.metrics.ServoToInfluxDbViaGraphiteNamingConvention.METRIC_FORMAT_7_ARGS;
import static com.expedia.www.haystack.metrics.ServoToInfluxDbViaGraphiteNamingConvention.MISSING_TAG;
import static com.expedia.www.haystack.metrics.ServoToInfluxDbViaGraphiteNamingConvention.TAG_KEY_SERVO_BUCKET;
import static com.expedia.www.haystack.metrics.ServoToInfluxDbViaGraphiteNamingConvention.TAG_KEY_STATISTIC;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_CLASS;
import static com.expedia.www.haystack.metrics.MetricObjects.TAG_KEY_SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ServoToInfluxDbViaGraphiteNamingConventionTest {
    private static final Random RANDOM = new Random();
    private static final String METRIC_NAME = RANDOM.nextLong() + "METRIC_NAME";
    private static final String METRIC_GROUP = RANDOM.nextLong() + "METRIC_GROUP";
    private static final String SUBSYSTEM = RANDOM.nextLong() + "SUBSYSTEM";
    private static final String APPLICATION = RANDOM.nextLong() + "APPLICATION";
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final String FULLY_QUALIFIED_CLASS_NAME = RANDOM.nextLong() + "FULLY_QUALIFIED_CLASS_NAME";
    private static final String CLASS = RANDOM.nextLong() + "CLASS";
    private static final String LINE_NUMBER = Integer.toString(RANDOM.nextInt(Integer.MAX_VALUE));
    private static final String TYPE = RANDOM.nextLong() + "TYPE";
    private static final String STATISTIC = RANDOM.nextLong() + "STATISTIC";
    private static final String TYPE_STATISTIC = TYPE + '_' + STATISTIC;
    private static final String LOCAL_HOST_NAME = "127.0.0.1";
    private static final String LOCAL_HOST_NAME_CLEANED = LOCAL_HOST_NAME.replace(".", "_");

    @Mock
    private InetAddress mockLocalHost;

    private ServoToInfluxDbViaGraphiteNamingConvention servoToInfluxDbViaGraphiteNamingConvention;

    @Before
    public void setUp() {
        servoToInfluxDbViaGraphiteNamingConvention = new ServoToInfluxDbViaGraphiteNamingConvention(LOCAL_HOST_NAME);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockLocalHost);
    }

    @Test
    public void testGetNameAllNull() {
        final Metric metric = new Metric(METRIC_NAME, BasicTagList.EMPTY, 0, 0);

        final String name = servoToInfluxDbViaGraphiteNamingConvention.getName(metric);

        final String expected = String.format(METRIC_FORMAT_6_ARGS,
                String.format(MISSING_TAG, TAG_KEY_SUBSYSTEM),
                String.format(MISSING_TAG, TAG_KEY_APPLICATION), LOCAL_HOST_NAME_CLEANED,
                String.format(MISSING_TAG, TAG_KEY_CLASS), METRIC_NAME,
                String.format(MISSING_TAG, DataSourceType.KEY));
        assertEquals(expected, name);
    }

    @Test
    public void testGetNameNoneNullWithoutStatistic() {
        final List<Tag> tagList = new ArrayList<>(4);
        tagList.add(Tags.newTag(TAG_KEY_SUBSYSTEM, SUBSYSTEM));
        tagList.add(Tags.newTag(TAG_KEY_APPLICATION, APPLICATION));
        tagList.add(Tags.newTag(TAG_KEY_CLASS, CLASS));
        tagList.add(Tags.newTag(DataSourceType.KEY, TYPE));
        final Metric metric = new Metric(METRIC_NAME, new BasicTagList(tagList), 0, 0);

        final String name = servoToInfluxDbViaGraphiteNamingConvention.getName(metric);

        assertEquals(String.format(
                METRIC_FORMAT_6_ARGS, SUBSYSTEM, APPLICATION, LOCAL_HOST_NAME_CLEANED, CLASS, METRIC_NAME, TYPE), name);
    }

    @Test
    public void testGetNameNoneNullWithStatistic() {
        final List<Tag> tagList = new ArrayList<>(5);
        tagList.add(Tags.newTag(TAG_KEY_SUBSYSTEM, SUBSYSTEM));
        tagList.add(Tags.newTag(TAG_KEY_APPLICATION, APPLICATION));
        tagList.add(Tags.newTag(TAG_KEY_CLASS, CLASS));
        tagList.add(Tags.newTag(DataSourceType.KEY, TYPE));
        tagList.add(Tags.newTag(TAG_KEY_STATISTIC, STATISTIC));
        final Metric metric = new Metric(METRIC_NAME, new BasicTagList(tagList), 0, 0);

        final String name = servoToInfluxDbViaGraphiteNamingConvention.getName(metric);

        final String expected = String.format(METRIC_FORMAT_6_ARGS,
                SUBSYSTEM, APPLICATION, LOCAL_HOST_NAME_CLEANED, CLASS, METRIC_NAME, TYPE_STATISTIC);
        assertEquals(expected, name);
    }

    @Test
    public void testGetNameErrorCase() {
        testGetNameCommonCode(METRIC_GROUP,
                TAG_KEY_FULLY_QUALIFIED_CLASS_NAME, FULLY_QUALIFIED_CLASS_NAME,
                TAG_KEY_LINE_NUMBER, LINE_NUMBER);
    }

    @Test
    public void testGetNameBucketCase() {
        testGetNameCommonCode(METRIC_GROUP_BUCKETS,
                TAG_KEY_APPLICATION, APPLICATION,
                TAG_KEY_SERVO_BUCKET, BUCKET);
    }

    private void testGetNameCommonCode(String metricGroup, String key1, String value1, String key2, String value2) {
        final List<Tag> tagList = new ArrayList<>(6);
        tagList.add(Tags.newTag(TAG_KEY_METRIC_GROUP, metricGroup));
        tagList.add(Tags.newTag(TAG_KEY_SUBSYSTEM, SUBSYSTEM));
        tagList.add(Tags.newTag(key1, value1));
        tagList.add(Tags.newTag(key2, value2));
        tagList.add(Tags.newTag(DataSourceType.KEY, TYPE));
        tagList.add(Tags.newTag(TAG_KEY_STATISTIC, STATISTIC));
        final Metric metric = new Metric(METRIC_NAME, new BasicTagList(tagList), 0, 0);

        final String name = servoToInfluxDbViaGraphiteNamingConvention.getName(metric);

        final String expected = String.format(METRIC_FORMAT_7_ARGS, metricGroup, SUBSYSTEM,
                value1, LOCAL_HOST_NAME_CLEANED, value2, METRIC_NAME, TYPE_STATISTIC);
        assertEquals(expected, name);
    }

}
