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
 * A trivial implementation of the GraphiteConfig interface, useful if not using cfg4j. (When using cfg4j, the
 * ConfigurationProvider.bind() method makes this GraphiteConfigImpl class unnecessary.)
 */
public class GraphiteConfigImpl implements GraphiteConfig {
    private final String host;
    private final int port;
    private final int pollintervalseconds;
    private final int queuesize;

    @SuppressWarnings("WeakerAccess")
    public GraphiteConfigImpl(String host, int port, int pollintervalseconds, int queuesize) {
        this.host = host;
        this.port = port;
        this.pollintervalseconds = pollintervalseconds;
        this.queuesize = queuesize;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public int pollintervalseconds() {
        return pollintervalseconds;
    }

    @Override
    public int queuesize() {
        return queuesize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphiteConfigImpl that = (GraphiteConfigImpl) o;

        if (port != that.port) return false;
        if (pollintervalseconds != that.pollintervalseconds) return false;
        //noinspection SimplifiableIfStatement
        if (queuesize != that.queuesize) return false;
        return host != null ? host.equals(that.host) : that.host == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + pollintervalseconds;
        result = 31 * result + queuesize;
        return result;
    }
}
