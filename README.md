Port of the JBoss AS 5 JMX Console to a generic App Server and JDK 8
===========

## Introduction

JBoss AS 5 had a web-based JMX console installed by default. 

It was available at `http://<hostname>/jmx-console`. 

Since JBoss 7 (and WildFly) the JMX console is no longer provided. 

This project contains a ported version of the old JMX console which can be deployed to JDK 8 compatible app servers.

## Building with Ant

1. Create a directory named `${user.home}/.m2/repository`
2. Copy the directories from `lib` to `${user.home}/.m2/repository`. Don't include `lib` but make sure that the rest of the directory structure is intact.
3. Run the command `ant clean war`
4. The WAR file will be generated at `target/jmxconsole-1.0.war`

## Building with Maven

Run the command `mvn clean package`

## Deploying the WAR

Deploy the WAR on your webapp server and point your browser at `http://<hostname>/jmxconsole-1.0` 

## Note!

Security is turned on by default. You will need to define at least one application user with the `jmx-console` group/role.



