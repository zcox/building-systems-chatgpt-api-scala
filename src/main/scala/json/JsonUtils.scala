package json

import cats.syntax.all._
import io.circe.{Json, ParsingFailure, Decoder, Error}
import io.circe.parser._

object JsonUtils {

  /** Best effort to check that there's likely a json object in the string, and
    * remove any junk around it.
    */
  def filter(s: String): Option[String] =
    (s.indexOf('{'), s.lastIndexOf('}')) match {
      case (i, j) if i >= 0 && j >= 0 && i < j =>
        s.substring(i, j + 1).some
      case _ =>
        none
    }

  def parseFiltered(s: String): Either[ParsingFailure, Json] =
    for {
      s2 <- filter(s).toRight(
        ParsingFailure(
          "String did not contain a json object",
          new NullPointerException(),
        )
      )
      j <- parse(s2)
    } yield j

  def parseAndDecode[A: Decoder](s: String): Either[Error, A] =
    for {
      j <- parseFiltered(s)
      a <- Decoder[A].decodeJson(j)
    } yield a

}
