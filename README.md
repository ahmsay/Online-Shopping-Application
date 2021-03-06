# Microshop
This is an online shopping application designed with microservices architecture.

<img src="https://user-images.githubusercontent.com/22731894/109545852-6f146980-7ada-11eb-8c1f-ad67a29643f2.png">

## Useful Scripts

### Run

`docker-compose up`

### Run with Detached Mode

`docker-compose up -d`

If you use detached mode, containers will be automatically removed after you stop the application.

### Stop

`docker-compose down`

### Remove All Containers

`docker rm $(docker ps -aq)`

### Push Images

`docker push <repository-name>:<tag>`

## Project Details

My main goal is to learn about microservices, so I kept the domain part of the project as simple as possible. The
project is about recording transactions of a shopping application.

### Client Services

There are four client services: account service, inventory service, payment service and order service. Each of them has
their own database.

### Spring Data

If you look into any client service, you won't see a repository implementation. There are only interfaces. This is the
work of Spring Data which has a bunch of utilities and makes CRUD operations a lot easier. Spring Data CRUD repositories
don't need an implementation. When you write your method by using a specific format, it is automatically resolved.

### Communication and Service Discovery

When a service needs to get information which is stored in another service, it sends an HTTP request to that service.
Therefore, the url of the service must be known but url depends on where the application is. It can be in a local
machine, or a container, or deployed in a server. So how do we know the correct url to send our request? Actually, we
don't need to. This is where service discovery comes in.

In this project, all other services register to the discovery server as discovery clients. Each of them has a name
located in their <b>bootstrap.yml</b> file. So instead of writing the url to send the request, we write the service
name. This encapsulates the varying service url. The discovery server knows the url, and the rest is just REST. It also
does load balancing which is crucial when you'll have multiple instances. There are server side and client side service
discoveries. I used Eureka, which makes client side discovery. We can also see registered services and their status on
discovery server. I registered the config server as well just to see its status. Also, if the discovery server goes
down, other instances will continue to communicate each other by using cache.

### Gateway Service

At this point, client services have different endpoints. This makes our API hard to use for our consumers. More
importantly, we can't use load balancing properly when we have multiple instances for the same service. Because each
instance will have a different endpoint, and we can't provide a new url to a consumer. To solve this problem, I included
a gateway service to the project and registered it to the discovery server. Now gateway service knows all other service
urls. Then I registered all client services to the gateway service by using service names. This way there is no url
dependency anymore, and I can use load balancing when a new instance is up. Also, our consumers won't even know whether
this is a microservice project or not, because all requests will go to the gateway service first, then the gateway
service will redirect them to the related microservice. Last but not least, I can handle authentication in this service
instead of implementing it in every client service.

### Configuration Server

Spring Framework provides some options to separate our configuration from code. This makes our application to run in
different environments without touching the code. But Spring Cloud Config Server takes it to another level. We can
update our configuration properties without restarting the application.

To do this, first I created a separate service and declared it as configuration server. Then I declared client services
as configuration clients. Spring Cloud Config provides a <b>Git</b> repository to store common properties and update
them dynamically. So I created a <a href="https://github.com/ahmsay/Microshop-Configuration" target="_blank">Git
repo</a> and gave its url to the config server. Now client services fetch data from the config server while starting.

So how do we update our config properties without restarting the application? Once we change a property in the Git repo,
config server automatically fetches it but client services will still get the old value. That's because it's not their
job to continuously ask the config server "Hey, are you updated?". To trigger them, we send a <b>refresh</b> request to
the intended bean. Then we get the updated property in run time.

### RabbitMQ

When a new payment is saved, a new order should be created with a default "Preparing" status. But orders are not in the
same database with payments. There can be a rest call to order service but rest calls work with request-response and
payment service doesn't need a response. Furthermore, this approach doesn't guarantee to keep databases synchronized
since order service might be down, and the request would be lost.

So I decided to use event driven approach with RabbitMQ to solve this problem. In this approach, microservices send
events when an action happens. When a new payment is saved, an event will be sent to order service through a message
broker. If order service is down, the event won't be lost. It can be processed later when the service is up again.

### Docker

Docker simply lets you run an application without relying on dependencies. All services have a Dockerfile to create
their images. Updating the jar and creating an image for 6 services was a-bit time-consuming, so I wrote
a <a href="https://github.com/ahmsay/Microshop/blob/master/project-configuration/build_images.sh" target="_blank">shell
script</a> to do it with one command.

### Docker Hub

Once images are created, I push them to my docker repositories with `docker push <repository-name>:<tag>` command.
Since `docker-compose push` doesn't work (god knows why) I push them separately.

https://hub.docker.com/u/ahmsay

### Docker Compose

Docker Compose lets you run multiple containers in the same network, with a simple command. So I created a two
docker-compose.yml files in project-configuration folder, for development and testing. In development mode only
databases and message queue will run as container, so I can keep debugging and developing the application. In test mode
whole application will run as containers, so I can verify if everything works correctly. If images don't exist in a
local device, they will be pulled.

Initially, there is a problem with docker configuration. Since the services start in a different network, they are not
running on localhost anymore. I did port mapping, but it is only for me to access them by using my local device. Client
services need to get the url of configuration server, discovery server and their databases' urls to run properly.

To solve this problem, I wrote an additional command in the <b>docker-compose.yml</b> file for client services to run
them with a different Spring profile. For example when I try to run the application with Docker Compose, account service
starts with a new Spring profile. Now I can override the discovery server url for that profile. The new url is the same
service name in the docker-compose.yml file: <b>discovery-server</b>. Spring is clever enough to resolve it. Same thing
also applies for config server and database. Now with the overridden urls, account service and other services can
connect to the discovery server, the config server and their databases. Furthermore, -thanks to Spring profiles- I don't
need to change my code to run the application in different environments.

So is it done now? NO! Client services first try to connect the config server, then the discovery server. When using
docker compose, all services start at the same time. That means the config server may not be ready when a client tries
to connect. That causes clients to fail to start. To fix it, I added <b>spring-retry</b> dependency to client services.
This solution was not working with <b>application.yml</b> files, so I started to use <b>bootstrap.yml</b>
files (Bootstrap context loads before the application context). Now if config server is not ready, clients try to
reconnect instead of shutting down. Once they connect to the config server, they'll try to connect the discovery server.
Fortunately, if the discovery server is not ready, they won't shut down and automatically try to reconnect.

In conclusion, to run this application with Docker Compose, you only need the <b>docker-compose.yml</b> file. When you
write `docker-compose up` command, after a bunch of errors (services desperately trying to connect each other) it will
run.

### Testing

Currently, there is no unit test or integration test but there
are <a href="https://github.com/ahmsay/Microshop/blob/master/project-configuration/microshop.postman_collection.json" target="_blank">
API tests</a>. You can import it in Postman to test the application.
