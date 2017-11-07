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

/**
 * Interface that glues configuration sources with code that needs and reads those configurations
 */
public interface GraphiteConfig {
    /**
     * IP address of the Graphite store that will receive Graphite messages
     *
     * @return the IP address or DNS name to use
     */
    String address();

    /**
     * Port of the Graphite store that will receive Graphite messages
     *
     * @return the port to use (typically 2003)
     */
    int port();

    /**
     * How often metric elements should be polled and sent to graphite
     *
     * @return the poll interval, in seconds
     */
    int pollintervalseconds();

    /**
     * The queue size of the asynchronous metric observer that polls metric data to send to Graphite
     *
     * @return the queue size to use
     */
    int queuesize();
}
