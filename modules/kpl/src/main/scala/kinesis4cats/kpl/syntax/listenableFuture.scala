/*
 * Copyright 2023-2023 etspaceman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kinesis4cats.kpl.syntax

import scala.util.control.NonFatal

import java.util.concurrent.{ExecutionException, Executor}

import cats.effect.Async
import com.google.common.util.concurrent.ListenableFuture

object listenableFuture extends ListenableFutureSyntax

trait ListenableFutureSyntax {
  implicit def toListenableFutureAsyncOps[F[_]](
      F: Async[F]
  ): ListenableFutureSyntax.ListenableFutureAsyncOps[F] =
    new ListenableFutureSyntax.ListenableFutureAsyncOps(F)
}

object ListenableFutureSyntax {
  final class ListenableFutureAsyncOps[F[_]](private val F: Async[F])
      extends AnyVal {

    /** Creates an [[cats.effect.Async Async]] instance from a
      * [[https://github.com/google/guava/blob/master/guava/src/com/google/common/util/concurrent/ListenableFuture.java ListenableFuture]]
      *
      * @see
      *   [[https://github.com/typelevel/cats-effect/discussions/2833 Gist]]
      * @param lf
      *   F of a
      *   [[https://github.com/google/guava/blob/master/guava/src/com/google/common/util/concurrent/ListenableFuture.java ListenableFuture]]
      * @return
      *   [[cats.effect.Async Async]] representing the result of the
      *   [[https://github.com/google/guava/blob/master/guava/src/com/google/common/util/concurrent/ListenableFuture.java ListenableFuture]]
      */
    def fromListenableFuture[A](lf: F[ListenableFuture[A]]) = F.async[A] { cb =>
      F.flatMap(F.executionContext) { ec =>
        F.flatMap(lf) { lf =>
          val executor: Executor = (command: Runnable) => ec.execute(command)
          F.delay {
            lf.addListener(
              () =>
                cb(
                  try Right(lf.get)
                  catch {
                    case ee: ExecutionException
                        if Option(ee.getCause).nonEmpty =>
                      Left(ee.getCause)
                    case NonFatal(e) => Left(e)
                  }
                ),
              executor
            )

            Some(F.flatMap(F.delay(lf.cancel(false))) {
              case true  => F.unit
              case false =>
                // failed to cancel - block until completion
                F.async_[Unit] { cb =>
                  lf.addListener(() => cb(Right(())), executor)
                }
            })
          }
        }
      }

    }
  }
}
