package template

import cats.Contravariant
import cats.data.Kleisli
import cats.syntax.all._
import cats.effect.Sync
import io.circe.{Json, Encoder}
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.{Template => HTemplate, _}

trait Template[F[_], A] {
  def apply(json: A): F[String]
}

object Template {

  def htemplate[F[_]: Sync](t: HTemplate): Template[F, String] =
    new Template[F, String] {
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

  def string[F[_]: Sync](template: String): Template[F, String] =
    htemplate(new Handlebars().compileInline(template))

  def file[F[_]: Sync](file: String): Template[F, String] =
    htemplate(new Handlebars().compile(file))

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

  def kleisli[F[_], A](t: Template[F, A]): Kleisli[F, A, String] =
    Kleisli(t(_))

  implicit class TemplateOps[F[_], A](t: Template[F, A]) {
    def K: Kleisli[F, A, String] =
      kleisli(t)
  }

  implicit class StringTemplateOps[F[_]](t: Template[F, String]) {
    def json: Template[F, Json] =
      t.contramap(_.noSpaces)
    def encoding[A: Encoder]: Template[F, A] =
      json.encoding[A]
  }

  implicit class JsonTemplateOps[F[_]](t: Template[F, Json]) {
    def encoding[A: Encoder]: Template[F, A] =
      t.contramap(Encoder[A].apply(_))
    def encodingK[A: Encoder]: Kleisli[F, A, String] =
      encoding.K
  }

}
