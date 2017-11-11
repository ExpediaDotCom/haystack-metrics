package com.expedia.www.haystack.metrics;

/**
 * A trivial implementation of the GraphiteConfig interface, useful if not using cfg4j. (When using cfg4j, the
 * ConfigurationProvider.bind() method makes this GraphiteConfigImpl class unnecessary.)
 */
public class GraphiteConfigImpl implements GraphiteConfig {
    private final String address;
    private final int port;
    private final int pollintervalseconds;
    private final int queuesize;

    @SuppressWarnings("WeakerAccess")
    public GraphiteConfigImpl(String address, int port, int pollintervalseconds, int queuesize) {
        this.address = address;
        this.port = port;
        this.pollintervalseconds = pollintervalseconds;
        this.queuesize = queuesize;
    }

    @Override
    public String address() {
        return address;
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
}
