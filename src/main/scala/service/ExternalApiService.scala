package service

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax.EncoderOps

class ExternalApiService(
    apiUrl: String
) {
  case class ExternalMovie(
      title: String,
      description: String,
      director: String,
      rt_score: String
  )

  // Here as well Future approach would be good (maybe better)
  @throws(classOf[java.io.IOException])
  @throws(classOf[java.net.SocketTimeoutException])
  def callAPI(
      connectTimeout: Int = 5000,
      readTimeout: Int = 5000,
      requestMethod: String = "GET"
  ): String = {
    import java.net.{HttpURLConnection, URL}
    val connection =
      new URL(apiUrl).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    val inputStream = connection.getInputStream
    val content = scala.io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    content
  }

  def getData: Either[String, String] =
    try {
      val content = callAPI()
      Right(content)
    } catch {
      case e: Exception => Left(e.getMessage)
    }

  // In both methods below it would be good to consider logging for problem troubleshooting
  def transformRawData: Either[Any, List[ExternalMovie]] =
    getData match {
      case Right(data) =>
        decode[List[ExternalMovie]](data)
      case Left(_) => Left("Problem with External API.")
    }

  def movies: Either[String, String] =
    transformRawData match {
      case Right(movieData) => Right(movieData.asJson.noSpaces)
      case Left(_)          => Left("Problem with External API.")
    }
}
