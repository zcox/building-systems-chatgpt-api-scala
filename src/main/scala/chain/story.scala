package chain

import template._
import openai._
import openai.syntax._
import json.JsonUtils.parseAndDecodeK
import json.syntax._
import cats.MonadThrow
import cats.data.Kleisli
import cats.effect._
import cats.syntax.all._
import io.circe.Decoder
import io.circe.generic.auto._

case class Person(
    first: String,
    last: String,
)

case class Story(
    title: String,
    text: String,
)

object Chain {
  def apply[F[_]: MonadThrow, A, B: Decoder](
      template: Template[F, A],
      llm: ChatCompletion[F],
  ): Kleisli[F, A, B] =
    template.K.andThen(llm.promptK).andThen(parseAndDecodeK[F, B])
}

case class StoryWriter[F[_]: Sync]() {

  // TODO need a better api for "a template that takes no input"

  val personTemplate = Template.file[F]("prompts/chain/person")
  val storyTemplate = Template.file[F]("prompts/chain/story").encoding[Person]

  def write(llm: ChatCompletion[F]): F[Story] =
    for {
      person <- llm.promptAs[String, Person](personTemplate, "")
      story <- llm.promptAs[Person, Story](storyTemplate, person)
    } yield story

  // template rendering, llm inference, json decoding, etc are kleislis, and we can compose those all up into one overall program/chain
  def writeK(llm: ChatCompletion[F]): F[Story] = {
    val generateRandomPerson = Chain[F, String, Person](personTemplate, llm)
    val writeStoryForPerson = Chain[F, Person, Story](storyTemplate, llm)
    val chain = generateRandomPerson.andThen(writeStoryForPerson)
    chain.run("")
  }

}
