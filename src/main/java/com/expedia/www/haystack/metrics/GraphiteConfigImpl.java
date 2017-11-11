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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphiteConfigImpl that = (GraphiteConfigImpl) o;

        if (port != that.port) return false;
        if (pollintervalseconds != that.pollintervalseconds) return false;
        //noinspection SimplifiableIfStatement
        if (queuesize != that.queuesize) return false;
        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + pollintervalseconds;
        result = 31 * result + queuesize;
        return result;
    }
}
