Port of the JBoss AS 5 JMX Console to Wildfly/EAP
===========

## Introduction

JBoss AS 5 had a web-based JMX console installed by default. 

It was available at `http://<hostname>/jmx-console`. 

Since JBoss 7 (and WildFly) the JMX console is no longer provided. 

This project contains a ported version of the old JMX console which can be deployed to Wildfly and to EAP.

## How to install

Build the WAR:

    mvn clean install

Then, deploy the WAR on WildFly or on EAP and point your browser at `http://<hostname>/jmx-console-1.0.0` 

## Note!

Security is turned on by default. You will need to define at least one user in the `ApplicationRealm` with the `jmx-console` group/role.



