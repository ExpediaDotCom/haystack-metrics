# Release Notes

## 0.2.1 / 2017-11-09 No functional changes
1. Add this ReleaseNotes.md file.
2. Use nexus-staging-maven-plugin in pom.xml to hopefully avoid manual release steps after upload.
 
## 0.2.0 / 2017-11-08 Change all configurations to have lower case only in the names (no camel case)
This was necessary because:
1. The configuration system used in Haystack is a mixture of environment variables and files.
2. The environment variables are by convention SCREAMING_SNAKE_CASE.
3. The configurations in the files (sometimes overridden by the environment variables) use period as a delimiter are
**not** upper case.
4. It would be difficult to write a converter that knew how to the environment variables to camel case.
Because of difficulties signing the jar file, this release was never sent to the SonaType Nexus repository.

## 0.1 / 2017-09-08 Initiaªl release to SonaType Nexus Repository