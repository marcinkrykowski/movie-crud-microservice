import cats.effect.{ContextShift, IO, Timer}
import config.Config
import io.circe.Json
import io.circe.literal._
import io.circe.optics.JsonPath._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class MovieServerSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with Eventually {
  private implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  private implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  private lazy val client = BlazeClientBuilder[IO](global).resource

  private val configFile = "test.conf"

  private lazy val config =
    Config.load(configFile).use(config => IO.pure(config)).unsafeRunSync()

  private lazy val urlStart =
    s"http://${config.server.host}:${config.server.port}"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(5, Seconds)),
    interval = scaled(Span(100, Millis))
  )

  override def beforeAll(): Unit = {
    HttpServer.create(configFile).unsafeRunAsyncAndForget()
    eventually {
      client
        .use(_.statusFromUri(Uri.unsafeFromString(s"$urlStart/movies")))
        .unsafeRunSync() shouldBe Status.Ok
    }
    ()
  }

  "Movie server" should {
    "create a movie" in {
      val title = "my movie 1"
      val rate = "Good"
      val createJson = json"""
        {
          "title": $title,
          "rate": $rate
        }"""
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"$urlStart/movies")
      ).withEntity(createJson)
      val json = client.use(_.expect[Json](request)).unsafeRunSync()
      root.id.long.getOption(json).nonEmpty shouldBe true
      root.title.string.getOption(json) shouldBe Some(title)
      root.rate.string.getOption(json) shouldBe Some(rate)
    }

    "update a movie" in {
      val id = createMovie("my movie 2", "Bad")

      val title = "updated movie"
      val rate = "Medium"
      val updateJson = json"""
        {
          "title": $title,
          "rate": $rate
        }"""
      val request = Request[IO](
        method = Method.PUT,
        uri = Uri.unsafeFromString(s"$urlStart/movies/$id")
      ).withEntity(updateJson)
      client.use(_.expect[Json](request)).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "title": $title,
          "rate": $rate
        }"""
    }

    "return a single movie" in {
      val title = "my movie 3"
      val rate = "Medium"
      val id = createMovie(title, rate)
      client
        .use(_.expect[Json](Uri.unsafeFromString(s"$urlStart/movies/$id")))
        .unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "title": $title,
          "rate": $rate
        }"""
    }

    "delete a movie" in {
      val title = "my movie 4"
      val rate = "Bad"
      val id = createMovie(title, rate)
      val deleteRequest = Request[IO](
        method = Method.DELETE,
        uri = Uri.unsafeFromString(s"$urlStart/movies/$id")
      )
      client
        .use(_.status(deleteRequest))
        .unsafeRunSync() shouldBe Status.NoContent

      val getRequest = Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(s"$urlStart/movies/$id")
      )
      client.use(_.status(getRequest)).unsafeRunSync() shouldBe Status.NotFound
    }

    "return all movies" in {
      // Remove all existing movies
      val json = client
        .use(_.expect[Json](Uri.unsafeFromString(s"$urlStart/movies")))
        .unsafeRunSync()
      root.each.id.long.getAll(json).foreach { id =>
        val deleteRequest = Request[IO](
          method = Method.DELETE,
          uri = Uri.unsafeFromString(s"$urlStart/movies/$id")
        )
        client
          .use(_.status(deleteRequest))
          .unsafeRunSync() shouldBe Status.NoContent
      }

      // Add new movies
      val title1 = "my movie 1"
      val title2 = "my movie 2"
      val rate1 = "Good"
      val rate2 = "Bad"
      val id1 = createMovie(title1, rate1)
      val id2 = createMovie(title2, rate2)

      // Retrieve movies
      client
        .use(_.expect[Json](Uri.unsafeFromString(s"$urlStart/movies")))
        .unsafeRunSync shouldBe json"""
        [
          {
            "id": $id1,
            "title": $title1,
            "rate": $rate1
          },
          {
            "id": $id2,
            "title": $title2,
            "rate": $rate2
          }
        ]"""
    }
  }

  private def createMovie(title: String, rate: String): Long = {
    val createJson = json"""
      {
        "title": $title,
        "rate": $rate
      }"""
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"$urlStart/movies")
    ).withEntity(createJson)
    val json = client.use(_.expect[Json](request)).unsafeRunSync()
    root.id.long.getOption(json).nonEmpty shouldBe true
    root.id.long.getOption(json).get
  }
}
