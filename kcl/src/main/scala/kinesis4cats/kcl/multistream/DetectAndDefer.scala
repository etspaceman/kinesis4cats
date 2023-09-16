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

import scala.concurrent.duration.FiniteDuration

import java.time.Duration

import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy.AutoDetectionAndDeferredDeletionStrategy

import kinesis4cats.compat.DurationConverters._

/** Basic implementation of the
  * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/FormerStreamsLeasesDeletionStrategy.java AutoDetectionAndDeferredDeletionStrategy]]
  *
  * @param deleteInterval
  *   [[https://www.scala-lang.org/api/2.13.12/scala/concurrent/duration/FiniteDuration.html FiniteDuration]]
  *   indicating the delay between lease deletion attempts
  */
final case class DetectAndDefer(deleteInterval: FiniteDuration)
    extends AutoDetectionAndDeferredDeletionStrategy {
  override val waitPeriodToDeleteFormerStreams: Duration =
    deleteInterval.toJava
}
