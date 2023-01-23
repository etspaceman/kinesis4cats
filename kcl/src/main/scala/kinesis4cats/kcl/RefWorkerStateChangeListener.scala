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

import cats.effect.std.Dispatcher
import cats.effect.{Async, Ref, Resource}
import software.amazon.kinesis.coordinator.WorkerStateChangeListener
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState

/** [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
  * implementation that provides the WorkerState in a [[cats.effect.Ref Ref]].
  *
  * @param dispatcher
  *   [[cats.effect.std.Dispatcher Dispatcher]] used to run effects
  * @param state
  *   [[cats.effect.Ref Ref]] of the WorkerState to update
  */
class RefWorkerStateChangeListener[F[_]] private[kinesis4cats] (
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

object RefWorkerStateChangeListener {

  /** Constuctor for
    * [[kinesis4cats.kcl.RefWorkerStateChangeListener RefWorkerStateChangeListener]]
    *
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.kcl.RefWorkerStateChangeListener RefWorkerStateChangeListener]]
    */
  def apply[F[_]: Async]: Resource[F, RefWorkerStateChangeListener[F]] = for {
    dispatcher <- Dispatcher.parallel[F]
    state <- Resource.eval(Ref.of(WorkerState.CREATED))
  } yield new RefWorkerStateChangeListener(dispatcher, state)
}
