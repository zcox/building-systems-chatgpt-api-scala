package openai

import cats._
import cats.syntax.all._
import cats.data.{Kleisli, OptionT}
import cats.effect._
import cats.effect.std.Env
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.circe.CirceEntityCodec._
import fs2.io.net.Network
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/*
https://platform.openai.com/docs/api-reference/chat/create
https://platform.openai.com/docs/api-reference/chat/object

TODO add support for function calls
 */

object Req {

  case class Message(
      role: String,
      content: String,
  )

  object Message {
    def system(content: String): Message = Message("system", content)
    def user(content: String): Message = Message("user", content)
    def assistant(content: String): Message = Message("assistant", content)
  }

  case class CreateChatCompletionRequest(
      model: String,
      messages: List[Message],
      // TODO [0, 2]
      temperature: Option[Double] = None,
      // TODO [0, 1]
      top_p: Option[Double] = None,
      // number of completions
      n: Option[Int] = None,
      max_tokens: Option[Int] = None,
      // TODO others...
  )

  def prompt(
      text: String,
      model: String = "gpt-3.5-turbo",
  ): CreateChatCompletionRequest =
    CreateChatCompletionRequest(
      model = model,
      messages = List(
        Message("user", text)
      ),
    )

  def promptK[F[_]: Applicative](
      model: String = "gtp-3.5-turbo"
  ): Kleisli[F, String, CreateChatCompletionRequest] =
    Kleisli.fromFunction[F, String](prompt(_, model))

  def chat(
      messages: List[Message],
      model: String = "gpt-3.5-turbo",
      temperature: Double = 0d,
      maxTokens: Int = 500,
  ): CreateChatCompletionRequest =
    CreateChatCompletionRequest(
      model = model,
      messages = messages,
      temperature = temperature.some,
      max_tokens = maxTokens.some,
    )

  def chat(messages: Message*): CreateChatCompletionRequest =
    chat(messages.toList)
}

object Resp {

  case class Message(
      role: String,
      content: String,
  )

  case class Choice(
      index: Int,
      message: Message,
      // stop, length, or function_call
      finish_reason: String,
  )

  case class Usage(
      prompt_tokens: Int,
      completion_tokens: Int,
      total_tokens: Int,
  )

  case class ChatCompletionResponse(
      id: String,
      `object`: String,
      created: Long,
      model: String,
      choices: List[Choice],
      usage: Usage,
  ) {
    def firstContentO: Option[String] =
      choices.headOption.map(_.message.content)
    def firstContent: String =
      firstContentO.getOrElse("")
  }

  def firstContentK[F[_]: Applicative]
      : Kleisli[F, ChatCompletionResponse, String] =
    Kleisli.fromFunction[F, ChatCompletionResponse](_.firstContent)
}

object OpenaiApiKey {
  val EnvVarName = "OPENAI_API_KEY"
  def fromEnv[F[_]: Sync]: F[Option[String]] =
    Env.make[F].get(EnvVarName)
  def fromEnvOrFail[F[_]: Sync]: F[String] =
    OptionT(fromEnv[F]).getOrRaise(
      new RuntimeException((s"Environment variable $EnvVarName not set"))
    )
}

trait ChatCompletion[F[_]] {
  def create(
      request: Req.CreateChatCompletionRequest
  ): F[Resp.ChatCompletionResponse]
}

case class ChatCompletionOps[F[_]: Functor](api: ChatCompletion[F]) {

  def prompt(prompt: String): F[String] =
    api
      .create(Req.prompt(prompt))
      .map(_.choices.headOption.map(_.message.content).getOrElse(""))

  def promptK: Kleisli[F, String, String] =
    Kleisli(prompt)

  def chat(messages: Req.Message*): F[String] =
    api
      .create(Req.chat(messages: _*))
      .map(_.choices.headOption.map(_.message.content).getOrElse(""))

  def chatK: Kleisli[F, Seq[Req.Message], String] =
    Kleisli(chat)

}

object ChatCompletion {

  val uri = uri"https://api.openai.com/v1/chat/completions"

  def apply[F[_]: Concurrent](
      client: Client[F],
      openaiApiKey: String,
  ): ChatCompletion[F] =
    new ChatCompletion[F] {
      override def create(
          request: Req.CreateChatCompletionRequest
      ): F[Resp.ChatCompletionResponse] =
        for {
          e <- request.asJson.pure[F]
          r = Request[F](
            method = Method.POST,
            uri = uri,
            headers = Headers(
              Authorization(Credentials.Token(AuthScheme.Bearer, openaiApiKey))
            ),
          ).withEntity(e)
          rsp <- client.expect[Resp.ChatCompletionResponse](r)
        } yield rsp
    }

  def simple[F[_]: Async: Network]: Resource[F, ChatCompletion[F]] =
    for {
      client <- EmberClientBuilder.default[F].build
      key <- Resource.eval(OpenaiApiKey.fromEnvOrFail[F])
      api = apply[F](client, key)
    } yield api

  type K[F[_]] =
    Kleisli[F, Req.CreateChatCompletionRequest, Resp.ChatCompletionResponse]

  def kleisli[F[_]](
      api: ChatCompletion[F]
  ): K[F] =
    Kleisli(api.create(_))
}

object Test extends IOApp.Simple {
  import openai.syntax._

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    for {
      (r1, r2) <- ChatCompletion
        .simple[IO]
        .use { api =>
          for {
            r1 <- api.prompt(
              "Take the letters in l-o-l-l-i-p-o-p and reverse them"
            )
            r2 <- api.chat(
              Req.Message.system(
                "You are an assistant who responds in the style of Dr Seuss. All your responses must be one sentence long."
              ),
              Req.Message.user("write me a story about a happy carrot"),
            )
          } yield (r1, r2)

        }
      () <- IO(println(r1))
      () <- IO(println(r2))
    } yield ()
}
