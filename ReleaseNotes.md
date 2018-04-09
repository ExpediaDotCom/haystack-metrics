# Release Notes

## 1.0.1 / 2018-04-09 No changes
Unable to upload 1.0.0 to Nexus, bumping the version and tagging before push in another upload attempt.

## 1.0.0 / 2018-04-06 Add support for BucketTimer and StatsTimer
Servo's
[BucketTimer](https://netflix.github.io/servo/current/servo-core/docs/javadoc/com/netflix/servo/monitor/BucketTimer.html)
and 
[StatsTimer](https://github.com/Netflix/servo/blob/master/servo-core/src/main/java/com/netflix/servo/monitor/StatsTimer.java)
provide different statistical possibilities than the BasicTimer already available from haystack-metrics.

## 0.10.0 / 2018-03-06 Make poll scheduler and poll scheduler count variables static
The poll scheduler variable is a singleton, vended by Servo, so that cached copy in MetricPublishing should be a static,
as should the variable that counts how many times MetricPublishing.start() was called.

## 0.9.0 / 2018-03-05 Count the number of times that MetricPublishing.start() was called
This is needed so that MetricPublishing.stop() will not actually stop the polling thread until the last call to 
MetricPublishing.start(); Log4j2 behavior at start-up necessitated this change.

## 0.8.0 / 2018-03-01 Log an informational message when starting metric publishing

## 0.7.0 / 2018-02-02 Perform environment variable substitution on GraphiteConfig.host()
Log4j2 parsing of YAML files does not perform environment variable substitution, so Haystack's use of the
[haystack-log4j-metrics-appender](https://github.com/ExpediaDotCom/haystack-log4j-metrics-appender) package wasn't
working properly without this change. Note this this means that, absent another fix, port cannot be specified in an
environment variable. (That fix is more extensive and will require treating port as a String until the last moment.)

## 0.6.0 / 2018-02-08 Ignore IllegalStateException in MetricPublishing.stop()
In case it wasn't started; this solves a problem seen when starting a Spring application that uses Logback. 

## 0.5.0 / 2018-01-09 Add new API with one more tag
This was needed for the ERROR_COUNTER use case in the custom logback/log4j appenders, to enable the identification of
subsystem in the metrics.

## 0.4.0 / 2017-12-15 Change new API to not include "NonRate" as part of its name
The renaming of the previous version was not quite complete; the API name (created in 0.2.9) needs to change as well.

## 0.3.0 / 2017-12-14 Add a configuration to control whether or not the metrics are all transformed to rates
The NetFlix rule is "all metrics are sent as rates"; this package allows, under configuration, counters to be sent
as straight counts. Also renamed ResettingNonRateCounter to ResettingCounter.

## 0.2.9 / 2017-12-12 Add MetricObjects.createResettingNonRateCounter()
Expose a method to allow the creation of the ResettingNonRateCounter provided in 0.2.8.

## 0.2.8 / 2017-12-12 Add ResettingNonRateCounter
The Netflix objects all return rates; the new returns a plain count, and a call to getValue() resets that count to 0.

## 0.2.7 / 2017-12-04 Change "address" to "port" in graphite configuration
This change makes configuration across Haystack packages using the same name for the graphite (InfluxDb) destination.

## 0.2.6 / 2017-12-01 Post at the specified polling interval, not twice that value
The 0.2.5 change accidentally did not get merged before committing and tagging.

## 0.2.5 / 2017-12-01 Post at the specified polling interval, not twice that value
There is a `pollintervalseconds` configuration needed, and previously the code multiplied that number by a constant
defined in the code (the value of that constant was 2), but this makes the metrics more difficult to understand.
Instead, just use the unmodified value of `pollintervalseconds` (typically set to 60).

## 0.2.4 / 2017-11-20 Only call pollScheduler.start() if it is not already started
As this haystack-metrics package is used more frequently and in different software layers, it's possible for multiple
callers to request that the metrics polling thread be started, but this call must only be made once for each
PollScheduler. (Typically there is only one PollScheduler, because `PollScheduler.getInstance()` is a singleton.)
So `MetricPublishing.start(GraphiteConfig)` now has a `synchronized (pollScheduler)` block in which 
`pollScheduler.isStarted()` is called, and the call to `pollScheduler.start()` is made if and only if
`pollScheduler.isStarted()` returns false.

## 0.2.3 / 2017-11-16 Add MetricPublishing.stop() method
Exposing a stop() method permits users of haystack-metrics to stop the metrics polling when required (for example, 
when shutting down the system).

## 0.2.2 / 2017-11-10 Add GraphiteConfigImpl
This new class makes it easier to use this haystack-metrics package without using cfg4j.

## 0.2.1 / 2017-11-09 No functional changes
1. Add this ReleaseNotes.md file.
2. Use nexus-staging-maven-plugin in pom.xml to hopefully avoid manual release steps after upload.
 
## 0.2.0 / 2017-11-08 Change all configurations to have lower case only in the names (no camel case)
This was necessary because:
1. The configuration system used in Haystack is a mixture of environment variables and files.
2. The environment variables are by convention SCREAMING_SNAKE_CASE.
3. The configurations in the files (sometimes overridden by the environment variables) use period as a delimiter and are
**not** upper case.
4. It would be difficult to write a converter that knows how to change the environment variables to camel case.
Because of difficulties signing the jar file, this release was never sent to the SonaType Nexus repository.

## 0.1 / 2017-09-08 Initial release to SonaType Nexus Repository