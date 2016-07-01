FantasyKitchen
==============

Example web project using Scala, Lift 2.6, and Riak.

Standalone setup
================
- Requires [Riak 1.4.x](#http://docs.basho.com/riak/1.4.12/quickstart/)
- Requires [Java 8](https://java.com/en/download/)
- Install riak
- [Update riak backend to use elevel db](http://docs.basho.com/riak/1.4.12/ops/advanced/backends/leveldb/#Installing-eLevelDB)
- Install and run backend api server.  Please refer to the [backend api readme](backend/README.md) for instructions on running and loading initial data.
- Install and run the front end web server using:
```
  cd clients/web
  sbt ~container:start
```

Docker setup
============
- Install [docker](https://docs.docker.com/engine/installation/) and [docker-compose](https://docs.docker.com/compose/install/)
- Run `docker-compose up`

How do I know it's working?
===========================
- Open a browser and visit http://localhost:8000
- The web server runs on port 8000, the api runs on port 8080, and riak will run by default on ports 8098 for HTTP traffic and 8087 for protocol buffers.