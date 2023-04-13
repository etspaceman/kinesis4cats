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

package kinesis4cats.kcl

import cats.effect._
import cats.effect.std.Dispatcher
import cats.syntax.all._
import software.amazon.kinesis.coordinator.WorkerStateChangeListener
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState

object WorkerListeners {

  /** [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
    * implementation that provides the WorkerState in a [[cats.effect.Ref Ref]].
    *
    * @param dispatcher
    *   [[cats.effect.std.Dispatcher Dispatcher]] used to run effects
    * @param state
    *   [[cats.effect.Ref Ref]] of the WorkerState to update
    */
  class RefListener[F[_]] private[kinesis4cats] (
      dispatcher: Dispatcher[F],
      val state: Ref[F, WorkerState]
  ) extends WorkerStateChangeListener {
    override def onWorkerStateChange(
        newState: WorkerState
    ): Unit = dispatcher.unsafeRunSync(
      state.set(newState)
    )
    override def onAllInitializationAttemptsFailed(e: Throwable): Unit = ()
  }

  object RefListener {

    /** Constuctor for
      * [[kinesis4cats.kcl.WorkerListeners.RefListener RefListener]]
      *
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[kinesis4cats.kcl.WorkerListeners.RefListener RefListener]]
      */
    def apply[F[_]: Async]: Resource[F, RefListener[F]] = for {
      dispatcher <- Dispatcher.parallel[F]
      state <- Resource.eval(Ref.of(WorkerState.CREATED))
    } yield new RefListener(dispatcher, state)
  }

  /** [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
    * implementation that completes a [[cats.effect.Deferred Deferred]] when the
    * consumer has reached a certain state.
    *
    * @param dispatcher
    *   [[cats.effect.std.Dispatcher Dispatcher]] used to run effects
    * @param state
    *   [[cats.effect.Ref Ref]] of the WorkerState to update
    * @param stateToCompleteOn
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    *   to expect when completing the [[cats.effect.Deferred Deferred]]
    */
  class DeferredListener[F[_]: Sync] private[kinesis4cats] (
      dispatcher: Dispatcher[F],
      val deferred: Deferred[F, Unit],
      stateToCompleteOn: WorkerState
  ) extends WorkerStateChangeListener {
    override def onWorkerStateChange(newState: WorkerState): Unit =
      if (newState == stateToCompleteOn)
        dispatcher.unsafeRunSync(
          deferred.complete(()).void
        )
      else ()

    override def onAllInitializationAttemptsFailed(e: Throwable): Unit = ()
  }

  object DeferredListener {

    /** Constuctor for
      * [[kinesis4cats.kcl.WorkerListeners.DeferredListener DeferredListener]]
      *
      * @param stateToCompleteOn
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
      *   to expect when completing the [[cats.effect.Deferred Deferred]].
      *   Default is STARTED.
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[kinesis4cats.kcl.WorkerListeners.DeferredListener DeferredListener]]
      */
    def apply[F[_]: Async](
        stateToCompleteOn: WorkerState = WorkerState.STARTED
    ): Resource[F, DeferredListener[F]] =
      for {
        dispatcher <- Dispatcher.parallel[F]
        deferredStarted <- Resource.eval(Deferred[F, Unit])
      } yield new DeferredListener(
        dispatcher,
        deferredStarted,
        stateToCompleteOn
      )
  }
}
