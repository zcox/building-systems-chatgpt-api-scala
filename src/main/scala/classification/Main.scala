package classification

import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import openai._
import template._

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val delimiter = "####"

  val systemMessage = Template
    .file[IO]("prompts/classification/system")
    .apply(s"""{"delimiter":"$delimiter"}""")

  object UserMessage {
    case class Context(
        delimiter: String,
        message: String,
    )
    object Context {
      def apply(message: String): Context = Context(delimiter, message)
    }
    val template = Template
      .string[IO](
        "{{delimiter}}{{message}}{{delimiter}}"
      )
      .encoding[Context]
    def apply(message: String): IO[String] =
      template(Context(message))
  }

  def input(message: String): IO[Req.CreateChatCompletionRequest] =
    for {
      sm <- systemMessage
      m <- UserMessage(message)
      r = Req.chat(
        Req.Message.system(sm),
        Req.Message.user(m),
      )
    } yield r

  def output(r: Resp.ChatCompletionResponse): String =
    r.choices.head.message.content

  def program(api: ChatCompletion[IO], message: String): IO[String] =
    for {
      i <- input(message)
      r <- api.create(i)
      o = output(r)
    } yield o

  def run: IO[Unit] =
    for {
      (r1, r2) <- ChatCompletion
        .simple[IO]
        .use(api =>
          for {
            r1 <- program(
              api,
              "I want you to delete my profile and all of my user data",
            )
            r2 <- program(api, "Tell me more about your flat screen tvs")
          } yield (r1, r2)
        )
      () <- IO(println(r1))
      () <- IO(println(r2))
    } yield ()

}
