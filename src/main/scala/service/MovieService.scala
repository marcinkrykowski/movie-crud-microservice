package service

import cats.effect.IO
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model.{Rate, Movie, MovieNotFoundError}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.{HttpRoutes, MediaType, Uri}
import repository.MovieRepository

class MovieService(
    repository: MovieRepository,
    externalApi: String = "https://ghibliapi.herokuapp.com/films"
) extends Http4sDsl[IO] {
  private implicit val encodeImportance: Encoder[Rate] =
    Encoder.encodeString.contramap[Rate](_.value)

  private implicit val decodeImportance: Decoder[Rate] =
    Decoder.decodeString.map[Rate](Rate.unsafeFromString)

  private val apiService = new ExternalApiService(externalApi)
//  private val moviesFromApi: String = apiService.movies

  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "allMovies" =>
      apiService.movies match {
        case Right(value)  => Ok(value)
        case Left(problem) => InternalServerError(problem)
      }
    case GET -> Root / "movies" =>
      Ok(
        Stream("[") ++ repository.getMovies
          .map(_.asJson.noSpaces)
          .intersperse(",") ++ Stream("]"),
        `Content-Type`(MediaType.application.json)
      )

    case GET -> Root / "movies" / LongVar(id) =>
      for {
        getResult <- repository.getMovie(id)
        response <- movieResult(getResult)
      } yield response

    case req @ POST -> Root / "movies" =>
      for {
        movie <- req.decodeJson[Movie]
        createdMovie <- repository.createMovie(movie)
        response <- Created(
          createdMovie.asJson,
          Location(Uri.unsafeFromString(s"/movies/${createdMovie.id.get}"))
        )
      } yield response

    case req @ PUT -> Root / "movies" / LongVar(id) =>
      for {
        movie <- req.decodeJson[Movie]
        updateResult <- repository.updateMovie(id, movie)
        response <- movieResult(updateResult)
      } yield response

    case DELETE -> Root / "movies" / LongVar(id) =>
      repository.deleteMovie(id).flatMap {
        case Left(MovieNotFoundError) => NotFound()
        case Right(_)                 => NoContent()
      }
  }

  private def movieResult(result: Either[MovieNotFoundError.type, Movie]) = {
    result match {
      case Left(MovieNotFoundError) => NotFound()
      case Right(movie)             => Ok(movie.asJson)
    }
  }
}
