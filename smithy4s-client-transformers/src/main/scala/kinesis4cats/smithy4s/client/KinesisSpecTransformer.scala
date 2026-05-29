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

import java.util.ArrayList
import java.util.Collections
import java.util.function
import java.util.function.BiFunction
import java.util.function.BiPredicate
import java.util.stream.Collectors

import software.amazon.smithy.build._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits._

final class KinesisSpecTransformer extends ProjectionTransformer {
  def getName() = "KinesisSpecTransformer"

  val metricsNameListShapeId =
    ShapeId.fromParts("com.amazonaws.kinesis", "MetricsNameList")

  val putRecordsOutputShapeId =
    ShapeId.fromParts("com.amazonaws.kinesis", "PutRecordsOutput")

  val listStreamConsumersOutputNextTokenShapeId =
    ShapeId.fromParts(
      "com.amazonaws.kinesis",
      "ListStreamConsumersOutput",
      "NextToken"
    )

  val listShardsOutputNextTokenShapeId =
    ShapeId.fromParts(
      "com.amazonaws.kinesis",
      "ListShardsOutput",
      "NextToken"
    )

  val nonNegativeIntegerObjectShape =
    IntegerShape
      .builder()
      .id(
        ShapeId.fromParts("com.amazonaws.kinesis", "NonNegativeIntegerObject")
      )
      .addTrait(RangeTrait.builder().min(java.math.BigDecimal.ZERO).build())
      .build()

  val traitTransform: BiFunction[Shape, Trait, Trait] =
    (shape: Shape, `trait`: Trait) =>
      if (
        `trait`.toShapeId() == LengthTrait.ID &&
        shape.toShapeId() == metricsNameListShapeId
      ) {
        LengthTrait.builder().min(0).max(7).build()
      } else if (
        shape
          .toShapeId() == listStreamConsumersOutputNextTokenShapeId && `trait`
          .toShapeId() == DocumentationTrait.ID
      ) {
        new DocumentationTrait(
          `trait`
            .asInstanceOf[DocumentationTrait] // scalafix:ok
            .getValue()
            .replace("ListStreamConsumersInput$NextToken", "NextToken")
        )
      } else if (
        shape
          .toShapeId() == listShardsOutputNextTokenShapeId && `trait`
          .toShapeId() == DocumentationTrait.ID
      ) {
        new DocumentationTrait(
          `trait`
            .asInstanceOf[DocumentationTrait] // scalafix:ok
            .getValue()
            .replace("ListShardsInput$NextToken", "NextToken")
        )
      } else `trait`

  val shapeTransform: function.Function[Shape, Shape] = (shape: Shape) =>
    if (shape.toShapeId() == putRecordsOutputShapeId) {
      val members =
        shape
          .getAllMembers()
          .entrySet()
          .stream()
          .map[MemberShape] { x =>
            val (memberName, member) = (x.getKey(), x.getValue())
            if (memberName == "FailedRecordCount")
              MemberShape
                .builder()
                .target(nonNegativeIntegerObjectShape.getId())
                .id(member.getId())
                .build()
            else member
          }
          .collect(Collectors.toList())

      shape
        .asStructureShape()
        .get()
        .toBuilder()
        .members(members)
        .build()
    } else shape

  // Traits present in the upstream AWS model that smithy4s would render as
  // Hints referencing namespaces we do not generate code for (they are not in
  // the codegen allowlist). Stripping their applications keeps the generated
  // client clean, matching the curated spec we previously consumed.
  //
  // NB: only Java APIs are used in field initializers here. This class is
  // compiled for Scala 3 but loaded into smithy4s' Scala 2.12 codegen
  // classloader, so touching the Scala collections library at construction
  // time would fail to instantiate the transformer.
  val smokeTestsTraitId =
    ShapeId.fromParts("smithy.test", "smokeTests")
  val waitableTraitId =
    ShapeId.fromParts("smithy.waiters", "waitable")

  val removableTraits: BiPredicate[Shape, Trait] =
    (_: Shape, `trait`: Trait) => {
      val id = `trait`.toShapeId()
      (id == smokeTestsTraitId) || (id == waitableTraitId)
    }

  def transform(context: TransformContext): Model = {
    val transformer = context.getTransformer()

    val withMappedTraits =
      transformer.mapTraits(context.getModel(), traitTransform)
    val newShapesAl = new ArrayList[Shape](
      withMappedTraits.shapes().collect(Collectors.toList())
    )
    newShapesAl.add(nonNegativeIntegerObjectShape)

    val newShapes = Collections.unmodifiableList(newShapesAl)

    val withMappedShapes = transformer.mapShapes(
      transformer.replaceShapes(withMappedTraits, newShapes),
      shapeTransform
    )

    transformer.removeTraitsIf(withMappedShapes, removableTraits)
  }

}
