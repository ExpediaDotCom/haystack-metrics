# Release Notes

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