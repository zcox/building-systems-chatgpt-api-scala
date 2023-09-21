package chain

import openai._
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    for {
      story <- ChatCompletion.simple[IO].use { api =>
        StoryWriter[IO]().writeK(api)
      }
      () <- IO(println(story.title))
      () <- IO(println("--------"))
      () <- IO(println(story.text))
    } yield ()

}
