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

import cats.effect.Async
import cats.effect.kernel.{Ref, Resource}
import cats.effect.syntax.all._
import cats.syntax.all._
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState
import software.amazon.kinesis.coordinator._

object KCLConsumer {
  def run[F[_]](
      config: KCLConsumerConfig[F]
  )(implicit F: Async[F]): Resource[F, Unit] = for {
    scheduler <- Resource.eval(
      F.delay(
        new Scheduler(
          config.checkpointConfig,
          config.coordinatorConfig,
          config.leaseManagementConfig,
          config.lifecycleConfig,
          config.metricsConfig,
          config.processorConfig,
          config.retrievalConfig
        )
      )
    )
    _ <- F
      .race(
        F.delay(scheduler.run()),
        if (config.raiseOnError)
          config.deferredException.get.flatMap(F.raiseError(_).void)
        else F.never
      )
      .toResource
    _ <- Resource.onFinalize(
      F.fromCompletableFuture(F.delay(scheduler.startGracefulShutdown())).void
    )
  } yield ()

  def runWithWorkerStateChangeListener[F[_]](
      config: KCLConsumerConfig[F]
  )(implicit
      F: Async[F]
  ): Resource[F, Ref[F, WorkerState]] = for {
    listener <- RefWorkerStateChangeListener[F]
    state <- Resource.pure(listener.state)
    _ <- run(
      config.copy(
        coordinatorConfig =
          config.coordinatorConfig.workerStateChangeListener(listener)
      )
    )
  } yield state
}
