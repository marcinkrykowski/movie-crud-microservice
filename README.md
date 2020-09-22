# Movie rating system
Example microservice written in Scala using some modern FP libraries.

*Disclaimer: This project serves as piece of my code. In production code there would be quite a few changes.*

#### Table of Contents  
* [Use case](#use-case)  
* [Endpoints](#endpoints)
* [Configuration](#configuration)
* [Running](#running)
* [Tests](#tests)
* [Libraries](#libraries)
    *  [http4s](#http4s)  
    *  [doobie](#doobie)  
    *  [circe](#circe)  
    *  [pureconfig](#pureconfig)  
* [Database](#database)
* [Academic discussion over some choices](#academic-discussion-over-some-choices)
    *  [Why I used tier architecture?](#why-i-used-tier-architecture)  
    *  [ Why I used http4s (library) over for example Play (framework)?](#why-i-used-http4s-library-over-for-example-play-framework)  
* [Possible improvements](#possible-improvements)

## Use case
Let's assume you want to watch movies and keep their rates in your local database, so other recommendation systems cannot know what are your favourite movies and so.
Although you want to fetch some example titles from external services, so you can pick whatever suits you best basing on other peoples' rates. 

That's how you can use that microservice. It performs basing CRUD operations for movies. You can create a movie with title and rate (Good, Medium, Bad).
## Endpoints
The rest endpoints listed below:

Method | Url          | Description
------ | -----------  | -----------
GET    | /allMovies   | Returns all movies from external source - [API](https://ghibliapi.herokuapp.com/films)
GET    | /movies      | Returns all movies from local database.
GET    | /movies/{id} | Returns movie for given id. Returns 404 when movie with passed id does not exist
POST   | /movies      | Creates a movies. Pass movie title and rate in JSON body. Returns a 201 with the created movie.
PUT    | /movies/{id} | Updates an existing movie. Pass movie title and rate in JSON body. Returns a 200 with the updated movie when a movie is present with the specified id, 404 otherwise.
DELETE | /movies/{id} | Deletes the movie with the specified id. Returns 404 when no movie present with passed id.

Some example requests: 

Get all movies from external recommendation system:
```curl http://localhost:8080/allMovies```

Create a movie:
```curl -X POST --header "Content-Type: application/json" --data '{"title": "Star Wars IV", "rate": "Good"}' http://localhost:8080/movies```

Get all movies from your local storage:
```curl http://localhost:8080/movies```

Get a single movie (assuming the id of the movie is 1):
```curl http://localhost:8080/movies/1```

Update a movie (assuming the id of the movie is 1):
```curl -X PUT --header "Content-Type: application/json" --data '{"title": "Star Wars IV"", "rate": "Good"}' http://localhost:8080/movies/1```

Delete a movie (assuming the id of the movie is 1):
```curl -X DELETE http://localhost:8080/movies/1```

## Configuration
All the configuration is stored in `application.conf`. By default, it listens to port number 8080 and uses Postgres database.

## Running
You can run the microservice with `sbt run` or by importing that project into your IDE and starting it from there.

## Tests
For testing purposes I use ScalaTest. Unit tests are using mocks, while integration tests are using real HTTP client to make requests.
To run unit tests do `sbt test`. To run integration tests `sbt it:test`.

## Libraries
### http4s
For REST [http4s](http://http4s.org/) is used. It provides streaming and functional HTTP for Scala.
Project uses [cats-effect](https://github.com/typelevel/cats-effect) as an effect monad, what postpones side effect till the last moment.

http4s uses [fs2](https://github.com/functional-streams-for-scala/fs2) for streaming. This allows to return
streams in the HTTP layer so the response doesn't need to be generated in memory before sending it to the client.

In the example project this is done for the `GET /movies` endpoint.

### doobie
As pure functional data access layer I used [doobie](http://tpolecat.github.io/doobie/).
This application uses [cats-effect](https://github.com/typelevel/cats-effect) with doobie,
although doobie can use another effect monad. That connection gives us pure functional solution.

### circe
[circe](https://github.com/circe/circe) is really easy and type safe JSON encoder/decoder that is built on top of Argonaut and cats.

### pureconfig
For configuration management I used [pureconfig](https://github.com/pureconfig/pureconfig). It basically reads from `application.conf` file into typed objects.

## Database
For local storage configuration I used [PostgreSQL](https://www.postgresql.org/). Typical relation database that is managed by doobie.

For integration testing I used in-memory database - [h2](http://www.h2database.com/). So the state of that is not kept anywhere. It's adjustable in `test.conf`.

Database migrations are done using [Flyway](https://flywaydb.org/) on the startup.

## Academic discussion over some choices
### Why I used tier architecture?
I'm totally aware of drawbacks of the tier architecture and its heavy dependency on database data model. Someone might ask: Why didn't you use hexagonal architecture? 
In production code I'd definitely consider that as we have integration with one external service, that might change to many external services in the future. 
That's why ports & adapters would be beneficial here for example in case we want to merge that data or calculate it somehow.
Despite that I decided to not over engineer that sample application and sticked to tier architecture assuming, that movie model will not evolve.
### Why I used http4s (library) over for example Play (framework)?
It is much better to have full control over what's happening in my application than rely on some runtime magic and annotations. That's why I've chosen http4s library over Play. 
Might have used [tapir](https://github.com/softwaremill/tapir), [typedApi](https://github.com/pheymann/typedapi) or [akka-http](https://github.com/akka/akka-http) as well.

## Possible improvements
- [ ] Add Swagger integration for API documenting 
- [ ] Add SQL syntax checks
- [ ] Add continuous integration
- [ ] Dockerize the application
- [ ] Add logging mechanism
- [ ] Add property based testing


