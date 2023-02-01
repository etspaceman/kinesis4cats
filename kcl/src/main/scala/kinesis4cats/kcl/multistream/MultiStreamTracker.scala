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
package multistream

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

import java.{util => ju}

import cats.effect.Async
import cats.syntax.all._
import software.amazon.kinesis.common.{InitialPositionInStreamExtended, StreamConfig, StreamIdentifier}
import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy
import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy._

import kinesis4cats.client.KinesisClient
import kinesis4cats.models.StreamArn

/** Basic implementation of the KCL
  * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/MultiStreamTracker.java MultiStreamTracker]]
  *
  * @param streamConfigList
  *   List of
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamConfig.java StreamConfig]]
  *   to consume
  * @param formerStreamsLeasesDeletionStrategy
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
  */
final class MultiStreamTracker(
    val streamConfigList: ju.List[StreamConfig],
    val formerStreamsLeasesDeletionStrategy: FormerStreamsLeasesDeletionStrategy
) extends software.amazon.kinesis.processor.MultiStreamTracker

object MultiStreamTracker {

  /** Basic constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    *
    * @param streams
    *   List of
    *   [[kinesis4cats.kcl.multistream.MultiStreamConfig MultiStreamConfig]] to
    *   consume
    * @param formerStreamsLeasesDeletionStrategy
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * @return
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def apply(
      streams: List[MultiStreamConfig],
      formerStreamsLeasesDeletionStrategy: FormerStreamsLeasesDeletionStrategy
  ): MultiStreamTracker =
    new MultiStreamTracker(
      streams.map(_.streamConfig).asJava,
      formerStreamsLeasesDeletionStrategy
    )

  /** Constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]].
    *
    * The
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
    * constructor for multi-stream instances requires users to provide a
    * creation-time epoch, which is not usually known. This describes the stream
    * to get that information. which allows users to provide values and a
    * [[kinesis4cats.client.KinesisClient KinesisClient]].
    *
    * @param client
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    * @param arns
    *   List of [[kinesis4cats.models.StreamArn StreamArn]] to consume
    * @param formerStreamsLeasesDeletionStrategy
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def fromArns[F[_]](
      client: KinesisClient[F],
      arns: Map[StreamArn, InitialPositionInStreamExtended],
      formerStreamsLeasesDeletionStrategy: FormerStreamsLeasesDeletionStrategy
  )(implicit F: Async[F]): F[MultiStreamTracker] = arns.toList
    .traverse { case (arn, position) =>
      MultiStreamConfig[F](client, arn, position)
    }
    .map(apply(_, formerStreamsLeasesDeletionStrategy))

  /** Basic constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]],
    * which hard-codes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * to the `NoLeaseDeletionStrategy` (meaning old leases will not be cleaned
    * up by the KCL)
    *
    * @param streams
    *   List of
    *   [[kinesis4cats.kcl.multistream.MultiStreamConfig MultiStreamConfig]] to
    *   consume
    * @return
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def noLeaseDeletion(streams: List[MultiStreamConfig]): MultiStreamTracker =
    apply(streams, new NoLeaseDeletionStrategy)

  /** Constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]],
    * which hard-codes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * to the `NoLeaseDeletionStrategy` (meaning old leases will not be cleaned
    * up by the KCL)
    *
    * The
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
    * constructor for multi-stream instances requires users to provide a
    * creation-time epoch, which is not usually known. This describes the stream
    * to get that information. which allows users to provide values and a
    * [[kinesis4cats.client.KinesisClient KinesisClient]].
    *
    * @param client
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    * @param arns
    *   List of [[kinesis4cats.models.StreamArn StreamArn]] to consume
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   F of
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def noLeaseDeletionFromArns[F[_]](
      client: KinesisClient[F],
      arns: Map[StreamArn, InitialPositionInStreamExtended]
  )(implicit F: Async[F]): F[MultiStreamTracker] =
    fromArns(client, arns, new NoLeaseDeletionStrategy)

  /** Basic constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]],
    * which hard-codes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * to the [[kinesis4cats.kcl.multistream.DetectAndDefer DetectAndDefer]]
    * strategy (meaning the KCL will attempt to detect leases to clean up and
    * delete them after a configured amount of time has passed)
    *
    * @param streams
    *   List of
    *   [[kinesis4cats.kcl.multistream.MultiStreamConfig MultiStreamConfig]] to
    *   consume
    * @param deleteInterval
    *   [[https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/FiniteDuration.html FiniteDuration]]
    *   indicating the delay between lease deletion attempts
    * @return
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def detectAndDefer(
      streams: List[MultiStreamConfig],
      deleteInterval: FiniteDuration
  ): MultiStreamTracker =
    apply(streams, new DetectAndDefer(deleteInterval))

  /** Constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]],
    * which hard-codes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * to the [[kinesis4cats.kcl.multistream.DetectAndDefer DetectAndDefer]]
    * strategy (meaning the KCL will attempt to detect leases to clean up and
    * delete them after a configured amount of time has passed)
    *
    * The
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
    * constructor for multi-stream instances requires users to provide a
    * creation-time epoch, which is not usually known. This describes the stream
    * to get that information. which allows users to provide values and a
    * [[kinesis4cats.client.KinesisClient KinesisClient]].
    *
    * @param client
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    * @param arns
    *   List of [[kinesis4cats.models.StreamArn StreamArn]] to consume
    * @param deleteInterval
    *   [[https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/FiniteDuration.html FiniteDuration]]
    *   indicating the delay between lease deletion attempts
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   F of
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def detectAndDeferFromArns[F[_]](
      client: KinesisClient[F],
      arns: Map[StreamArn, InitialPositionInStreamExtended],
      deleteInterval: FiniteDuration
  )(implicit F: Async[F]): F[MultiStreamTracker] =
    fromArns(client, arns, new DetectAndDefer(deleteInterval))

  /** Basic constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]],
    * which hard-codes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * to the [[kinesis4cats.kcl.multistream.ProvideAndDefer ProvideAndDefer]]
    * strategy (meaning the user will provide a means to detect streams to clean
    * up, and the KCL will delete them after a configured amount of time has
    * passed)
    *
    * @param streams
    *   List of
    *   [[kinesis4cats.kcl.multistream.MultiStreamConfig MultiStreamConfig]] to
    *   consume
    * @param deleteInterval
    *   [[https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/FiniteDuration.html FiniteDuration]]
    *   indicating the delay between lease deletion attempts
    * @param identifyStreamsToDelete
    *   User-defined function to produce a list of
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
    *   values to clean up
    * @return
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def provideAndDefer(
      streams: List[MultiStreamConfig],
      deleteInterval: FiniteDuration
  )(identifyStreamsToDelete: () => List[StreamIdentifier]): MultiStreamTracker =
    apply(
      streams,
      new ProvideAndDefer(deleteInterval)(
        identifyStreamsToDelete
      )
    )

  /** Constructor for
    * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]],
    * which hard-codes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java FormerStreamsLeasesDeletionStrategy]]
    * to the [[kinesis4cats.kcl.multistream.ProvideAndDefer ProvideAndDefer]]
    * strategy (meaning the user will provide a means to detect streams to clean
    * up, and the KCL will delete them after a configured amount of time has
    * passed)
    *
    * The
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
    * constructor for multi-stream instances requires users to provide a
    * creation-time epoch, which is not usually known. This describes the stream
    * to get that information. which allows users to provide values and a
    * [[kinesis4cats.client.KinesisClient KinesisClient]].
    *
    * @param client
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    * @param arns
    *   List of [[kinesis4cats.models.StreamArn StreamArn]] to consume
    * @param deleteInterval
    *   [[https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/FiniteDuration.html FiniteDuration]]
    *   indicating the delay between lease deletion attempts
    * @param identifyStreamsToDelete
    *   User-defined function to produce a list of
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
    *   values to clean up
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   F of
    *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
    */
  def provideAndDeferFromArns[F[_]](
      client: KinesisClient[F],
      arns: Map[StreamArn, InitialPositionInStreamExtended],
      deleteInterval: FiniteDuration
  )(
      identifyStreamsToDelete: () => List[StreamIdentifier]
  )(implicit F: Async[F]): F[MultiStreamTracker] =
    fromArns(
      client,
      arns,
      new ProvideAndDefer(deleteInterval)(identifyStreamsToDelete)
    )

}
