package template

import cats.effect.Sync
import io.circe.{Json, Encoder}

object Handlebars {

  def apply[F[_]: Sync](template: String, json: String): F[String] =
    Template.string[F](template).apply(json)

  def apply[F[_]: Sync](template: String, json: Json): F[String] =
    Template.string[F](template).json(json)

  def apply[F[_]: Sync, A: Encoder](template: String, a: A): F[String] =
    Template.string[F](template).encoding[A].apply(a)

}
