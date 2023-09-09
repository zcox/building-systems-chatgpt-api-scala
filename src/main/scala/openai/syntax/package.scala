package openai

import cats.Functor

package object syntax {

  implicit def chatCompletionOps[F[_]: Functor](
      chat: ChatCompletion[F]
  ): ChatCompletionOps[F] =
    ChatCompletionOps[F](chat)

}
