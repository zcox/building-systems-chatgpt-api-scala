package json

import template._
import openai._
import json.syntax._
import cats._
import cats.data.Kleisli
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

  def promptAsK[A: Decoder]: Kleisli[F, String, A] =
    Kleisli(promptAs[A])

  def promptAs[A, B: Decoder](t: Template[F, A], a: A): F[B] =
    for {
      p <- t(a)
      b <- promptAs[B](p)
    } yield b

  def chatAs[A: Decoder](ms: Req.Message*): F[A] =
    for {
      r <- api.create(Req.chat(ms: _*))
      c <- r.choices.headOption
        .liftTo[F](new NullPointerException("No choices in response"))
      a <- c.message.contentAs[A].liftTo[F]
    } yield a

}
