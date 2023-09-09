package json

import cats._
import openai._

package object syntax {
  implicit def richRespMessage(m: Resp.Message): RichRespMessage =
    RichRespMessage(m)
  implicit def richChatCompletion[F[_]: MonadThrow](
      c: ChatCompletion[F]
  ): RichChatCompletion[F] =
    RichChatCompletion(c)
}
