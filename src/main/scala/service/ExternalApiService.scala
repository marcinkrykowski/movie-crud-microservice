package service

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.http4s.dsl.Http4sDsl

class ExternalApiService(
    apiUrl: String
) extends Http4sDsl[IO] {
  case class ExternalMovie(
      title: String,
      description: String,
      director: String,
      rt_score: String
  )

  def callAPI: String = {
    val source = scala.io.Source.fromURL(apiUrl)
    val data = source.mkString
    source.close()
    data
  }

  def transformRawData: Either[io.circe.Error, List[ExternalMovie]] =
    decode[List[ExternalMovie]](callAPI)

  def movies: String =
    transformRawData match {
      case Right(movieData) => movieData.asJson.noSpaces
      case Left(error)      => "Problem with external api call. " + error
    }
}
