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

package kinesis4cats.kcl.multistream

import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

import java.time.Duration
import java.{util => ju}

import software.amazon.kinesis.common.StreamIdentifier
import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy.ProvidedStreamsDeferredDeletionStrategy

/** Basic implementation of the
  * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java ProvidedStreamsDeferredDeletionStrategy]]
  *
  * @param deleteInterval
  *   [[https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/FiniteDuration.html FiniteDuration]]
  *   indicating the delay between lease deletion attempts
  * @param identifyStreamsToDelete
  *   User-defined function to produce a list of
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamIdentifier.java StreamIdentifier]]
  *   values to clean up
  */
final case class ProvideAndDefer(
    deleteInterval: FiniteDuration
)(identifyStreamsToDelete: () => List[StreamIdentifier])
    extends ProvidedStreamsDeferredDeletionStrategy {
  override val waitPeriodToDeleteFormerStreams: Duration =
    deleteInterval.toJava
  override def streamIdentifiersForLeaseCleanup(): ju.List[StreamIdentifier] =
    identifyStreamsToDelete().asJava
}
