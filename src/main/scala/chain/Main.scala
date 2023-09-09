package chain

import openai._
import json.syntax._
import cats._
import cats.syntax.all._
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._

case class Person(
    first: String,
    last: String,
)

trait PersonGen[F[_]] {
  def random: F[Person]
}

object PersonGen {

  def llm[F[_]: MonadThrow](api: ChatCompletion[F]): PersonGen[F] =
    new PersonGen[F] {
      override def random: F[Person] =
        api.promptAs[Person](
          "Make up a person's first and last name. Return only a json object with first and last fields."
        )
    }

}

case class Story(
    title: String,
    text: String,
)

trait StoryGen[F[_]] {
  def random(person: Person): F[Story]
}

object StoryGen {

  def llm[F[_]: MonadThrow](api: ChatCompletion[F]): StoryGen[F] =
    new StoryGen[F] {
      override def random(person: Person): F[Story] =
        // TODO handlebars
        api.promptAs[Story](
          s"""You are a talented author of children's stories. Write a short story about ${person.first} ${person.last}.
             |Return only a json object with the following fields:
             |title: the title of your short story
             |text: the short story
             |Make sure you return only a valid json object with these fields.""".stripMargin
        )
    }

}

object StoryWriter {

  def write[F[_]: MonadThrow](llm: ChatCompletion[F]): F[Story] =
    for {
      person <- llm.promptAs[Person](
        "Make up a person's first and last name. Return only a json object with first and last fields."
      )
      story <- llm.promptAs[Story](
        s"""You are a talented author of children's stories. Write a short story about ${person.first} ${person.last}.
             |Return only a json object with the following fields:
             |title: the title of your short story
             |text: the short story
             |Make sure you return only a valid json object with these fields.""".stripMargin
      )
    } yield story

}

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    for {
      story <- ChatCompletion.simple[IO].use { api =>
        for {
          p <- PersonGen.llm[IO](api).random
          s <- StoryGen.llm[IO](api).random(p)
        } yield s
      }
      () <- IO(println(story.title))
      () <- IO(println("--------"))
      () <- IO(println(story.text))
    } yield ()

}
