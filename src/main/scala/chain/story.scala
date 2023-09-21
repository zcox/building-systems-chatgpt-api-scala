package chain

import openai._
import json.syntax._
import cats._
import cats.syntax.all._
import io.circe.generic.auto._

case class Person(
    first: String,
    last: String,
)

case class Story(
    title: String,
    text: String,
)

object StoryWriter {

  def write[F[_]: MonadThrow](llm: ChatCompletion[F]): F[Story] =
    for {
      person <- llm.promptAs[Person](
        "Make up a person's first and last name. Return only a json object with first and last fields."
      )
      story <- llm.promptAs[Story](
        s"""You are a talented author of children's stories. Write a 3-paragraph short story about ${person.first} ${person.last}.
           |Return only a json object with the following fields:
           |title: the title of your short story
           |text: the short story
           |Make sure you return only a valid json object with these fields.""".stripMargin
      )
    } yield story

}
