package service

import cats.effect.IO
import fs2.Stream
import io.circe.Json
import io.circe.literal._
import model.{High, Low, Medium, Movie}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, Response, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.MovieRepository

class MovieServiceSpec extends AnyWordSpec with MockFactory with Matchers {
  private val repository = stub[MovieRepository]

  private val service = new MovieService(repository).routes
  private val serviceWithWrongExternalUrl =
    new MovieService(repository, "https://ghibliapi.herokuapp.pl/films").routes

  "MovieService" should {
    "return error from external api" in {
      val response = serviceWithWrongExternalUrl
        .orNotFound(Request[IO](GET, Uri.unsafeFromString(s"/allMovies")))
        .unsafeRunSync()
      response.status shouldBe Status.InternalServerError
      response.as[String].unsafeRunSync() should contain
      println(response.body.toString())
      """  Problem with External API. """
    }
    "return list of movies from external api" in {
      val response =
        serve(Request[IO](GET, Uri.unsafeFromString(s"/allMovies")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() should contain
      """  "title": "Grave of the Fireflies" """
    }
    "create a movie" in {
      val id = 1
      val movie = Movie(None, "my movie", Low)
      (repository.createMovie _)
        .when(movie)
        .returns(IO.pure(movie.copy(id = Some(id))))
      val createJson = json"""
        {
          "title": ${movie.title},
          "rate": ${movie.rate.value}
        }"""
      val response =
        serve(Request[IO](POST, uri"/movies").withEntity(createJson))
      response.status shouldBe Status.Created
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "title": ${movie.title},
          "rate": ${movie.rate.value}
        }"""
    }

    "update a movie" in {
      val id = 1
      val movie = Movie(None, "updated movie", Medium)
      (repository.updateMovie _)
        .when(id, movie)
        .returns(IO.pure(Right(movie.copy(id = Some(id)))))
      val updateJson = json"""
        {
          "title": ${movie.title},
          "rate": ${movie.rate.value}
        }"""

      val response = serve(
        Request[IO](PUT, Uri.unsafeFromString(s"/movies/$id"))
          .withEntity(updateJson)
      )
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "title": ${movie.title},
          "rate": ${movie.rate.value}
        }"""
    }

    "return a single movie" in {
      val id = 1
      val movie = Movie(Some(id), "my movie", High)
      (repository.getMovie _).when(id).returns(IO.pure(Right(movie)))

      val response =
        serve(Request[IO](GET, Uri.unsafeFromString(s"/movies/$id")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "title": ${movie.title},
          "rate": ${movie.rate.value}
        }"""
    }

    "return all movies" in {
      val id1 = 1
      val movie1 = Movie(Some(id1), "my movie 1", High)
      val id2 = 2
      val movie2 = Movie(Some(id2), "my movie 2", Medium)
      val movies = Stream(movie1, movie2)
      (repository.getMovies _).when().returns(movies)

      val response = serve(Request[IO](GET, uri"/movies"))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        [
         {
           "id": $id1,
           "title": ${movie1.title},
           "rate": ${movie1.rate.value}
         },
         {
           "id": $id2,
           "title": ${movie2.title},
           "rate": ${movie2.rate.value}
         }
        ]"""
    }

    "delete a movie" in {
      val id = 1
      (repository.deleteMovie _).when(id).returns(IO.pure(Right(())))

      val response =
        serve(Request[IO](DELETE, Uri.unsafeFromString(s"/movies/$id")))
      response.status shouldBe Status.NoContent
    }
  }

  private def serve(request: Request[IO]): Response[IO] = {
    service.orNotFound(request).unsafeRunSync()
  }
}
