[![Coverage Status](https://coveralls.io/repos/github/ExpediaDotCom/haystack-metrics/badge.svg?branch=master)](https://coveralls.io/github/ExpediaDotCom/haystack-metrics?branch=master)
# haystack-metrics
This haystack-metrics module contains code needed by most or all of the other modules in the Haystack code base.

## Metrics
The Haystack system is deployed by [Kubernetes](https://en.wikipedia.org/wiki/Kubernetes) and comes with an
[InfluxDb](https://www.influxdata.com/time-series-platform/influxdb/) database for time series data (TSD).
Other modules then use Netflix [Servo](https://github.com/Netflix/servo) metrics objects to create two types of metrics:
1. [Counter](https://github.com/Netflix/servo/blob/master/servo-core/src/main/java/com/netflix/servo/monitor/Counter.java)
monitors to track how often an event of interest is occurring, and
2. [Timer](https://github.com/Netflix/servo/blob/master/servo-core/src/main/java/com/netflix/servo/monitor/Timer.java)
monitors to track how much time an event of interest is taking.

Glue code in the [metrics](src/main/java/com/expedia/www/haystack/metrics) package of this module makes it easy to 
create Counters and Timers.
### Usage
#### Dependencies
In the `<properties>` section of pom.xml put:
```
<haystack-metrics-version>...</haystack-metrics-version>
```
You should of course use the correct version of the haystack-metrics dependency in place of ... above.

In the `<dependencies>` section of pom.xml put:
```
<dependency>
    <groupId>com.expedia.www</groupId>
    <artifactId>haystack-metrics</artifactId>
    <version>${haystack-metrics-version}</version>
</dependency>
```
#### How to create objects
In the examples below, the values of `SUBSYSTEM`, `APPLICATION`, and `CLASS_NAME` should not contain spaces or periods 
(each period or space will be changed to a hyphen).
##### Subsystem
As you will see, creating a Servo object in Haystack requires a "subsystem" String, whose value will be something like
"pipes" or "trends"; the `SUBSYSTEM` constant below should be defined at a high level in your subsystem code base.
```
public static final String SUBSYSTEM = "subsystemName"; // e.g. "pipes" or "trends"
```
##### Application
Applications are in the subsystem's git repository, and good practice is to store the application name at a high level
in the application's code hierarchy.
```
public static final String SUBSYSTEM = "applicationName";
```
##### Class
Creating a Servo object also requires a "class" String, which is often the Java class or Scala object containing the
object:
```
private static final String CLASS_NAME = ClassContainingTheCounter.class.getSimpleName();
```
Refactoring or renaming may well lead to changing the name of the Java class or Scala object in which the Servo object
resides, so it also acceptable to choose a "class" String that will never change:
```
private static final String CLASS_NAME = "JsonSerialization";
```
##### Singleton
Your Servo objects should be singletons, either as static (Java) or object (Scala) variables. The MetricObjects
variable with which you create them can be managed by a Dependency Injection (DI) framework or not, as you see fit.
(The examples below show the creation of a new MetricsObject with the creation of each Servo object.) 
Servo objects are specified with Identifiers:
1. Subsystem
2. Application
3. Class Name
4. Metric Name
If a Counter or Timer is created twice (that is, its Identifiers match that of an already registered Counter or Timer),
then a warning will be logged and the existing object returned by the createAndRegister call. A Timer and a Counter 
with matching Identifiers is allowed but best avoided.
#### Counter
##### Creation
The code below is a Java snippet that shows the right way to create a Counter:
```
static final Counter REQUEST = (new MetricObjects()).createAndRegisterCounter(
    SUBSYSTEM, APPLICATION, CLASS_NAME, "REQUEST");
```
Because the Servo Counter generates a RATE metric, using upper case for the variable name `REQUEST` and the counter name 
`"REQUEST"` is recommended because doing so results in an sensibly named complete metric name of `REQUEST_RATE` in
InfluxDb, as explained in the "Graphite Bridge" section of this document. The `sendasrate`configuration controls whether
rates or counts are sent by the Counters (simple counts are easier to understand and often just as useful as rates).
##### Usage
Simply increment the Counter to count:
```
REQUEST.increment();
```
It will be reset when its value is reported to InfluxDb.
#### BasicTimer
##### Creation
The code below is a Java snippet that shows the right way to create a BasicTimer:
```
static final Timer JSON_SERIALIZATION = (new MetricObjects()).createAndRegisterTimer(
    SUBSYSTEM, APPLICATION, CLASS_NAME, "JSON_SERIALIZATION", TimeUnit.MICROSECONDS);
```
The Servo Timer generates four metrics (GAUGE max, NORMALIZED count, NORMALIZED totalOfSquares, and NORMALIZED
totalTime), and while using upper case is again suggested (see the Counter section above), the complete metric names 
(`JSON_SERIALIZATION_GAUGE_min`, `JSON_SERIALIZATION_NORMALIZED_count`, `JSON_SERIALIZATION_NORMALIZED_totalOfSquares`, 
and `JSON_SERIALIZATION_NORMALIZED_totalTime`) are mixed case.
Choose the appropriate time unit as the last argument:
* For on-host code, `TimeUnit.MICROSECONDS` is probably appropriate.
* For network calls, `TimeUnit.MILLISECONDS` may be sufficient.
The coarser TimeUnit.MILLISECONDS has less performance impact than the finer TimeUnit.MICROSECONDS and 
TimeUnit.NANOSECONDS; you can read more about this issue
[here](https://stackoverflow.com/questions/19052316/why-is-system-nanotime-way-slower-in-performance-than-system-currenttimemill)
and [here](http://stas-blogspot.blogspot.nl/2012/02/what-is-behind-systemnanotime.html).
##### Usage
Follow the pattern below (this is for Java; the Scala implementation is similar):
```
final Stopwatch stopwatch = JSON_SERIALIZATION.start();
try {
    // Do the work being timed
} finally {
    stopwatch.stop();
}
```
You can also do your own timing without using a Stopwatch:
```
JSON_SERIALIZATION.record(timeItTookInMs, TimeUnit.MILLISECONDS);
```
Again, the Timer will be reset when its values are reported to InfluxDb.
#### The Main Method
To initialize the metrics system, the first line of your main() method should be something like:
```
(new MetricPublishing()).start(graphiteConfig);
```
where graphiteConfig is an implementation of the GraphiteConfig interface declared in this module.
#### Configuration
You will typically have a base.yaml in your resources directory whose contents will include something like:
```
haystack:
  graphite:
     prefix: "haystack" # using something other than "haystack" will require a change in the InfluxDb template
     host: "haystack.local" # set in /etc/hosts per instructions in haystack/deployment module
     port: 2003 # Graphite port; typically 2003
     pollintervalseconds: 60
     queuesize: 10
     sendasrate: false
```
### Graphite Bridge
The "Graphite Bridge" connects Servo metrics from the application to the Haystack InfluxDb via Graphite 
[plaintext protocol](http://graphite.readthedocs.io/en/latest/feeding-carbon.html#the-plaintext-protocol) messages.
Such a message consists of three space-delimited Strings terminated by a newline:
```
 <metric path> <metric value> <metric timestamp>\n
```
The `<metric value>` is a number, and the pieces of `<metric path>` are traditionally separated by a period.
Note that the period-delimited pieces contain no metadata; that is, the meanings of each piece are not specified in the
message. This lack of metadata is addressed in [OpenTSDB](http://opentsdb.net) but code to connect Servo metrics to
InfluxDb via the OpenTSDB protocol does not currently exist in Servo; instead, the bridge uses the Graphite plaintext 
protocol, and an InfluxDb template (read about them in this 
[README](https://github.com/influxdata/influxdb/blob/master/services/graphite/README.md) file) parses the Graphite plain
text message into tags. (You can read about metrics tags 
[here](http://opentsdb.net/docs/build/html/user_guide/query/timeseries.html).)

This graphite bridge therefore requires a convention to map each metric piece to a tag; this convention is found/used in 
three places that must agree on the convention:
1. The template configuration (see the `templates` value in 
[influxdb.yaml](https://github.com/ExpediaDotCom/haystack/blob/master/deployment/k8s/addons/1.6/monitoring/influxdb.yaml))
2. The code that builds client-side tags (see `getTags()` in 
[MetricObjects.java](src/main/java/com/expedia/www/haystack/metrics/MetricObjects.java))
3. The code that creates the graphite plain text message from the metric and its client-side tags (see `getName()` in 
[ServoToInfluxDbViaGraphiteNamingConvention.java](src/main/java/com/expedia/www/haystack/metrics/ServoToInfluxDbViaGraphiteNamingConvention.java))

As a result, the graphite message has the following meaning:
```
<system>.<server>.<subsystem>.<application>.<class>.<VARIABLE_NAME>_<METRIC_NAME> (for Counter)
<system>.<server>.<subsystem>.<application>.<class>.<VARIABLE_NAME>_<METRIC_NAME>_<timerStatName> (for Timer)
```
where:
* `<system>` is typically "haystack" (this value is controlled by the `haystack.graphite.prefix` configuration)
* `<server>` is the host name
* `<subsystem>` is the value discussed in the "Subsystem" section above
* `<application>` is the value discussed in the "Application" section above
* `<class>` is the  value discussed in the "Class" section above
* `<VARIABLE_NAME>_<METRIC_NAME>` or `<VARIABLE_NAME>_<METRIC_NAME>_<timerStatName>` is the complete metric name; see 
the "Counter" and "BasicTimer" sections above.

### Releases
1. Decide what kind of version bump is necessary, based on [Semantic Versioning](http://semver.org/) conventions.
In the items below, the version number you select will be referred to as `x.y.z`.
2. Update the [pom.xml](https://github.com/ExpediaDotCom/haystack-metrics/blob/master/pom.xml), changing the
version element to `<version>x.y.z-SNAPSHOT</version>`. Note the `-SNAPSHOT` suffix.
3. Make your code changes, including unit tests. This package requires 100% unit test code coverage for the build to 
succeed.
4. Update the [ReleaseNotes.md]((https://github.com/ExpediaDotCom/haystack-metrics/blob/master/ReleaseNotes.md)) file
with details of your changes.
5. Create a pull request with your changes.
6. Ask for a review of the pull request; when it is approved, the Travis CI build will upload the resulting jar file
to the [SonaType Staging Repository](https://oss.sonatype.org/#stagingRepositories)
7. Tag the build with the version number: from a command line, executed in the root directory of the project:
```
git tag x.y.z
git push --tags
```
This will cause the jar file to be released to the 
[SonaType Release Repository](https://oss.sonatype.org/#nexus-search;quick~haystack-metrics)