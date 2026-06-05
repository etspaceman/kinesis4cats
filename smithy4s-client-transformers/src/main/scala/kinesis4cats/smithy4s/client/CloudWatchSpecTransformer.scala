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

/** Strips protocol traits from the CloudWatch service that smithy4s would render
  * as Hints referencing namespaces we do not generate code for. The upstream AWS
  * CloudWatch model tags the service with `smithy.protocols#rpcv2Cbor`, whose
  * generated `smithy.protocols.Rpcv2Cbor` Hint is not on our classpath (we only
  * pull the `aws.protocols` traits via smithy4s-aws). Removing the trait leaves
  * the `aws.protocols#awsQuery` / `awsJson1_0` protocols smithy4s-aws supports.
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

  val removableTraits: BiPredicate[Shape, Trait] =
    (_: Shape, `trait`: Trait) => `trait`.toShapeId() == rpcv2CborTraitId

  def transform(context: TransformContext): Model = {
    val transformer = context.getTransformer()
    transformer.removeTraitsIf(context.getModel(), removableTraits)
  }

}
