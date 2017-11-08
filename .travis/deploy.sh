#!/bin/bash
cd `dirname $0`/.. 

if [ -z "$SONATYPE_USERNAME" ]
then
    echo "ERROR! Please set SONATYPE_USERNAME and SONATYPE_PASSWORD environment variable"
    exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ]
then
    echo "ERROR! Please set SONATYPE_PASSWORD environment variable"
    exit 1
fi


if [ ! -z "$TRAVIS_TAG" ]
then
    export AGENT_JAR_VERSION=$TRAVIS_TAG
    SKIP_GPG_SIGN=false
    echo "travis tag is set -> updating pom.xml <version> attribute to $TRAVIS_TAG"
    mvn --settings .travis/settings.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=$TRAVIS_TAG 1>/dev/null 2>/dev/null
else
    SKIP_GPG_SIGN=true
    echo "no travis tag is set, so publishing the current snapshot versioned jar without gpg signing"
fi

mvn clean deploy --settings .travis/settings.xml -Dgpg.skip=$SKIP_GPG_SIGN -DskipTests=true -B -U

echo "successfully deployed the jars to nexus"
