package template

import cats.Contravariant
import cats.syntax.all._
import cats.effect.Sync
import io.circe.{Json, Encoder}
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars._

trait Template[F[_], A] {
  def apply(json: A): F[String]
}

object Template {

  def string[F[_]: Sync](template: String): Template[F, String] =
    new Template[F, String] {
      val t = new Handlebars().compileInline(template)
      override def apply(json: String): F[String] =
        Sync[F].delay {
          t.apply(
            Context
              .newBuilder(new ObjectMapper().readTree(json))
              .resolver(JsonNodeValueResolver.INSTANCE)
              .build()
          )
        }
    }

  implicit def contravariant[F[_]]: Contravariant[Template[F, *]] =
    new Contravariant[Template[F, *]] {
      override def contramap[A, B](
          fa: Template[F, A]
      )(f: B => A): Template[F, B] =
        new Template[F, B] {
          override def apply(json: B): F[String] =
            fa.apply(f(json))
        }
    }

  def json[F[_]: Sync](template: String): Template[F, Json] =
    string[F](template).contramap(_.noSpaces)

  def encoding[F[_]: Sync, A: Encoder](template: String): Template[F, A] =
    json[F](template).contramap(Encoder[A].apply(_))

}

object Handlebars {

  def apply[F[_]: Sync](template: String, json: String): F[String] =
    Sync[F].delay {
      // TODO if none of this performs any I/O, then reduce Sync
      // TODO can we reuse ObjectMapper, Handlebars, etc? thread-safe?
      new Handlebars()
        .compileInline(template)
        .apply(
          Context
            .newBuilder(new ObjectMapper().readTree(json))
            .resolver(JsonNodeValueResolver.INSTANCE)
            .build()
        )
    }

  def apply[F[_]: Sync](template: String, json: Json): F[String] =
    apply[F](template, json.noSpaces)

  def apply[F[_]: Sync, A: Encoder](template: String, a: A): F[String] =
    apply[F](template, Encoder[A].apply(a))

}
