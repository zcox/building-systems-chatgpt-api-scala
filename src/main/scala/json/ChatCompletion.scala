package json

import openai._
import json.syntax._
import cats._
import cats.syntax.all._
import io.circe.Decoder

case class RichChatCompletion[F[_]: MonadThrow](api: ChatCompletion[F]) {

  def promptAs[A: Decoder](s: String): F[A] =
    for {
      r <- api.create(Req.prompt(s))
      c <- r.choices.headOption
        .liftTo[F](new NullPointerException("No choices in response"))
      a <- c.message.contentAs[A].liftTo[F]
    } yield a

}
