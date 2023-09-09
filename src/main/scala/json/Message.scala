package json

import openai._
import io.circe.{Decoder, Error}

case class RichRespMessage(m: Resp.Message) {

  def contentAs[A: Decoder]: Either[Error, A] =
    JsonUtils.parseAndDecode[A](m.content)

}
