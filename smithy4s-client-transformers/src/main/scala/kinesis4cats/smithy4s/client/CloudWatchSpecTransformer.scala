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

package kinesis4cats.smithy4s.client

import java.util.function.BiPredicate

import software.amazon.smithy.build._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits._

/** Pins the CloudWatch service to the `aws.protocols#awsQuery` protocol by
  * stripping the other protocol traits the upstream AWS model declares:
  *
  *   - `smithy.protocols#rpcv2Cbor` — its generated `smithy.protocols.Rpcv2Cbor`
  *     Hint references a namespace we do not generate code for (we only pull the
  *     `aws.protocols` traits via smithy4s-aws), so it fails to compile.
  *   - `aws.protocols#awsJson1_0` — smithy4s-aws would otherwise prefer this
  *     (higher priority than awsQuery), serialising requests as JSON. We want the
  *     XML query protocol smithy4s-aws supports for CloudWatch.
  *   - `aws.protocols#awsQueryCompatible` — only meaningful alongside a JSON
  *     protocol; removed for cleanliness once awsJson is gone.
  *
  * NB: only Java APIs are used in field initializers here. This class is
  * compiled for Scala 3 but loaded into smithy4s' Scala 2.12 codegen
  * classloader, so touching the Scala collections library at construction time
  * would fail to instantiate the transformer.
  */
final class CloudWatchSpecTransformer extends ProjectionTransformer {
  def getName() = "CloudWatchSpecTransformer"

  val rpcv2CborTraitId =
    ShapeId.fromParts("smithy.protocols", "rpcv2Cbor")
  val awsJson1_0TraitId =
    ShapeId.fromParts("aws.protocols", "awsJson1_0")
  val awsQueryCompatibleTraitId =
    ShapeId.fromParts("aws.protocols", "awsQueryCompatible")

  val removableTraits: BiPredicate[Shape, Trait] =
    (_: Shape, `trait`: Trait) => {
      val id = `trait`.toShapeId()
      (id == rpcv2CborTraitId) ||
      (id == awsJson1_0TraitId) ||
      (id == awsQueryCompatibleTraitId)
    }

  def transform(context: TransformContext): Model = {
    val transformer = context.getTransformer()
    transformer.removeTraitsIf(context.getModel(), removableTraits)
  }

}
