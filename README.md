# loom-docker-adapter
Loom integration with Docker. For more information on Loom look at its [github repo](https://github.com/HewlettPackard/loom).  This adapter is packaged for deployment by [loom-docker](https://github.com/HewlettPackard/loom-docker)

Tested with docker-ce 17.06.0-ce and spotify docker client 8.8.1.

Build process creates a single shaded jar `target/docker-adapter.jar` that needs to be referenced by a running loom instance along with a customised adapter properties file - see the example  `docker.properties`.
