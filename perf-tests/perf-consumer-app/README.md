# Simple Kafka Consumer App 

## Implementation Approach


## Building your application

```
mvn install
```

Your `pom.xml` file is already configured to add your REST application to the OpenLiberty `defaultServer`. 

The `install-apps` goal copies the application into the specified directory of the specified server.
In this case, the goal copies the `PerfConsumerApp.war` file into the `apps` directory of the `defaultServer` server.

## Building the image

To build your image, make sure that your Docker daemon is running and execute the Docker `build` command
from the command line. If you execute your build from the same directory as your Dockerfile, you can
use the period character (`.`) notation to specify the location for the build context. Otherwise, use
the `-f` flag to point to your Dockerfile:

```
docker build -t ibmcase/perfconsumerapp .
```

The first build usually takes much longer to complete than subsequent builds because Docker needs to
download all dependencies that your image requires, including the parent image.


## Running your application in Docker container

Now that your image is built, execute the Docker `run` command with the absolute path to this guide:

```
docker run -d --name perfconsumerapp -p 9080:9080 -p 9443:9443 -v $(pwd)/target/liberty/wlp/usr/servers:/servers ibmcase/perfconsumerapp
```

## Testing the container

Before you access your application from the browser, run the `docker ps` command from the command line to make sure that your container is running and didn't crash:

```
$ docker ps
CONTAINER ID        IMAGE               CREATED             STATUS              NAMES
2720cea71700        ibmcase/perfconsumerapp          2 seconds ago       Up 1 second   perfconsumerapp
```

To view a full list of all available containers, run the `docker ps -a` command from the command line.

If your container is running without problems, point your browser to http://localhost:9080/LibertyProject/System/properties[http://localhost:9080/LibertyProject/System/properties^],
where you can see a JSON file that contains the system properties of the JVM in your container.
[role="code_command hotspot", subs="quotes"]
----
#Update the `PropertiesResource` class.#
`src/main/java/io/openliberty/guides/rest/PropertiesResource.java`
----

[role="edit_command_text"]
Change the endpoint of your application from `properties` to `properties-new` by changing the [hotspot=Path]`@Path`
annotation to `"properties-new"`.


To see these changes reflected in the container, run the following command from the command line to
rebuild your application and point your browser to http://localhost:9080/LibertyProject/System/properties-new[http://localhost:9080/LibertyProject/System/properties-new^].
[role='command']
```
mvn package
```
You see the same JSON file that you saw previously.

To stop your container, run the following command from the command line.
[role='command']
```
docker stop rest-app
```

If a problem occurs and your container exits prematurely, the container won't appear in the container
list that the `docker ps` command displays. Instead, your container appears with an `Exited`
status when you run the `docker ps -a` command. Run the `docker logs rest-app` command to view the
container logs for any potential problems and double-check that your Dockerfile is correct. When you
find the cause of the issue, remove the faulty container with the `docker rm rest-app` command, rebuild
your image, and start the container again.

PropertiesResource.java
[source, Java, linenums, role='code_column hide_tags=comment']
----
include::finish/src/main/java/io/openliberty/guides/rest/PropertiesResource.java[]
----

== Great work! You're done!

You have learned how to set up, run, iteractively develop a simple REST application in a container with
Open Liberty and Docker. Whenever you make changes to your application code, they will now reflect
automatically on the Open Liberty server running in a container when the application rebuilds.



include::{common-includes}/attribution.adoc[subs="attributes"]
