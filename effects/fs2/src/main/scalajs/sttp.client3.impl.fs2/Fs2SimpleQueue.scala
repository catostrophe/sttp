package sttp.client3.impl.fs2

import cats.MonadError
import cats.effect.std.Dispatcher
import cats.syntax.flatMap._
import cats.effect.std.Queue
import sttp.client3.internal.ws.SimpleQueue
import sttp.ws.WebSocketBufferFull

class Fs2SimpleQueue[F[_], A](queue: Queue[F, A], capacity: Option[Int], dispatcher: Dispatcher[F])(implicit
    F: MonadError[F, Throwable]
) extends SimpleQueue[F, A] {
  override def offer(t: A): Unit = {
    // On the JVM, we do unsafeRunSync. Here this is not possible, so just starting a future and leaving it running,
    // without waiting for it to complete (the `offer` contract allows that).
    dispatcher.unsafeToFuture(
      queue
        .tryOffer(t)
        .flatMap[Unit] {
          case true  => F.unit
          case false => F.raiseError(new WebSocketBufferFull(capacity.getOrElse(Int.MaxValue)))
        }
    )
  }

  override def poll: F[A] = queue.take
}
