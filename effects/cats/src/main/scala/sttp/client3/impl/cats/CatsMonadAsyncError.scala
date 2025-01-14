package sttp.client3.impl.cats

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.option._
import sttp.monad.{Canceler, MonadAsyncError}

class CatsMonadAsyncError[F[_]](implicit F: Async[F]) extends MonadAsyncError[F] {
  override def unit[T](t: T): F[T] = F.pure(t)

  override def map[T, T2](fa: F[T])(f: T => T2): F[T2] = F.map(fa)(f)

  override def flatMap[T, T2](fa: F[T])(f: T => F[T2]): F[T2] =
    F.flatMap(fa)(f)

  override def error[T](t: Throwable): F[T] = F.raiseError(t)

  override protected def handleWrappedError[T](rt: F[T])(h: PartialFunction[Throwable, F[T]]): F[T] =
    F.recoverWith(rt)(h)

  override def async[T](register: ((Either[Throwable, T]) => Unit) => Canceler): F[T] =
    F.async(cb => F.delay(register(cb)).map(c => F.delay(c.cancel()).some))

  override def eval[T](t: => T): F[T] = F.delay(t)

  override def flatten[T](ffa: F[F[T]]): F[T] = F.flatten(ffa)

  override def ensure[T](f: F[T], e: => F[Unit]): F[T] = F.guaranteeCase(f)(_ => e)
}
