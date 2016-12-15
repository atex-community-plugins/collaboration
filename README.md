collaboration
================

This plugin allow polopoly to publish publication and workflow events to a slack api compatible third party.

In order for the plugin to be available you need to add the configuration dependency to your top POM.
```
  <dependency>
    <groupId>com.atex.plugins</groupId>
    <artifactId>collaboration</artifactId>
    <version>1.0-SNAPSHOT</version>
  </dependency>

  <dependency>
    <groupId>com.atex.plugins</groupId>
    <artifactId>collaboration</artifactId>
    <version>1.0-SNAPSHOT</version>
    <classifier>contentdata</classifier>
  </dependency>
```

The actual dispatching of events happens in the integration server, so you need to make sure it can connects to jboss jms events.
Add the jboss client libraries to `server-integration/pom.xml`:

```
  <dependency>
    <groupId>jboss</groupId>
    <artifactId>jboss-client-libraries</artifactId>
    <version>4.0.5</version>
    <type>pom</type>
    <exclusions>
      <exclusion>
        <groupId>jboss</groupId>
        <artifactId>concurrent</artifactId>
      </exclusion>
    </exclusions>
  </dependency>
```

Then you need to enable the jms component, in `server-integration/config/dev/applicationContext.xml` make sure you have:

```
  <bean id="jbossmq" class="org.apache.camel.component.jms.JmsComponent">
    <property name="connectionFactory" ref="jbossConnectionFactory"/>
  </bean>

  <jee:jndi-lookup id="jbossConnectionFactory" jndi-name="ConnectionFactory">
    <jee:environment>
      java.naming.provider.url=jnp://${jboss.hostname}:${jboss.naming.port}
      java.naming.factory.initial=org.jboss.naming.NamingContextFactory
    </jee:environment>
  </jee:jndi-lookup>
```

And finally enable the JMS events flow in the same file:

```
  <bean id="contentEventProcessor" class="com.atex.plugins.collaboration.ContentEventProcessor"/>

  <camelContext xmlns="http://camel.apache.org/schema/spring">

  ...

    <route>
      <from uri="jbossmq:topic:jms/PolopolyTopic" />
      <process ref="contentEventProcessor" />
    </route>

  </camelContext>
```

To disable/enable the plugin or add a specific configuration go to plugins configurations under the Root Deparment plugins configurations.

## Docker

If you want to quickly run a local instance of MatterMost you can use docker:

```
docker run --name mattermost-preview -d --publish 8065:8065 mattermost/mattermost-preview
```

In this way a local instance of mattermost suitable for developments and demo will be available at http://$DOCKER_HOST:8065/

## Polopoly Version
10.16.3-fp1

## Code Status
The code in this repository is provided with the following status: **EXAMPLE**

Under the open source initiative, Atex provides source code for plugin with different levels of support. There are three different levels of support used. These are:

- EXAMPLE  
The code is provided as an illustration of a pattern or blueprint for how to use a specific feature. Code provided as is.

- PROJECT  
The code has been identified in an implementation project to be generic enough to be useful also in other projects. This means that it has actually been used in production somewhere, but it comes "as is", with no support attached. The idea is to promote code reuse and to provide a convenient starting point for customization if needed.

- PRODUCT  
The code is provided with full product support, just as the core Polopoly product itself.
If you modify the code (outside of configuraton files), the support is voided.


## License
Atex Polopoly Source Code License
Version 1.0 February 2012

See file **LICENSE** for details
