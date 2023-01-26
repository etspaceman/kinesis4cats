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

import cats.effect.kernel.Deferred
import cats.effect.syntax.all._
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState
import software.amazon.kinesis.coordinator._

/** Primary entrypoint for running a consumer
  */
object KCLConsumer {

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]
    *
    * @param config
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] containing the
    *   required configuration
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
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

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]. This exposes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    * which can be used as a means to determine if the KCL Consumer is running.
    * A common use case for this is to service healthcheck endpoints when
    * running these in an orchestration service (e.g. Kubernetes or AWS ECS) or
    * running tests that require the consumer to be up before an assertion is
    * checked.
    *
    * @param config
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] containing the
    *   required configuration
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  def runWithRefListener[F[_]](
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

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]. This exposes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    * which can be used as a means to determine if the KCL Consumer is running.
    * A common use case for this is to service healthcheck endpoints when
    * running these in an orchestration service (e.g. Kubernetes or AWS ECS) or
    * running tests that require the consumer to be up before an assertion is
    * checked.
    *
    * Unlike `runWithRefListener`, this method uses a
    * [[cats.effect.Deferred Deferred]] instance. This is useful when you only
    * need to react when the KCL has reached a specific state 1 time (e.g. wait
    * to produce to a stream until the consumer is started)
    *
    * @param config
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] containing the
    *   required configuration
    * @param stateToCompleteOn
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    *   to expect when completing the [[cats.effect.Deferred Deferred]]. Default
    *   is STARTED.
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  def runWithDeferredListener[F[_]](
      config: KCLConsumerConfig[F],
      stateToCompleteOn: WorkerState = WorkerState.STARTED
  )(implicit
      F: Async[F]
  ): Resource[F, Deferred[F, Unit]] = for {
    listener <- DeferredWorkerStateChangeListener[F](stateToCompleteOn)
    deferred <- Resource.pure(listener.deferred)
    _ <- run(
      config.copy(
        coordinatorConfig =
          config.coordinatorConfig.workerStateChangeListener(listener)
      )
    )
  } yield deferred
}
