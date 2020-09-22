package repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import model.{Rate, Movie, MovieNotFoundError}
import doobie._
import doobie.implicits._

class MovieRepository(transactor: Transactor[IO]) {
  private implicit val rateMeta: Meta[Rate] =
    Meta[String].timap(Rate.unsafeFromString)(_.value)

  def getMovies: Stream[IO, Movie] = {
    sql"SELECT id, title, rate FROM movie"
      .query[Movie]
      .stream
      .transact(transactor)
  }

  def getMovie(id: Long): IO[Either[MovieNotFoundError.type, Movie]] = {
    sql"SELECT id, title, rate FROM movie WHERE id = $id"
      .query[Movie]
      .option
      .transact(transactor)
      .map {
        case Some(movie) => Right(movie)
        case None        => Left(MovieNotFoundError)
      }
  }

  def createMovie(movie: Movie): IO[Movie] = {
    sql"INSERT INTO movie (title, rate) VALUES (${movie.title}, ${movie.rate})".update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(transactor)
      .map { id =>
        movie.copy(id = Some(id))
      }
  }

  def deleteMovie(id: Long): IO[Either[MovieNotFoundError.type, Unit]] = {
    sql"DELETE FROM movie WHERE id = $id".update.run.transact(transactor).map {
      affectedRows =>
        if (affectedRows == 1) {
          Right(())
        } else {
          Left(MovieNotFoundError)
        }
    }
  }

  def updateMovie(
      id: Long,
      movie: Movie
  ): IO[Either[MovieNotFoundError.type, Movie]] = {
    sql"UPDATE movie SET title = ${movie.title}, rate = ${movie.rate} WHERE id = $id".update.run
      .transact(transactor)
      .map { affectedRows =>
        if (affectedRows == 1) {
          Right(movie.copy(id = Some(id)))
        } else {
          Left(MovieNotFoundError)
        }
      }
  }
}
