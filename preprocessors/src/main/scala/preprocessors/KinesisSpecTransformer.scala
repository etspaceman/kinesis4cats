package preprocessors

import scala.jdk.CollectionConverters._

import java.util.function
import java.util.function.BiFunction

import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.IntegerShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.LengthTrait
import software.amazon.smithy.model.traits.RangeTrait
import software.amazon.smithy.model.traits.Trait

final class KinesisSpecTransformer extends ProjectionTransformer {
  def getName() = "KinesisSpecTransformer"

  private val metricsNameListShapeId =
    ShapeId.fromParts("com.amazonaws.kinesis", "MetricsNameList")

  private val putRecordsOutputShapeId =
    ShapeId.fromParts("com.amazonaws.kinesis", "PutRecordsOutput")

  private val nonNegativeIntegerObjectShape =
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
      } else `trait`

  val shapeTransform: function.Function[Shape, Shape] = (shape: Shape) =>
    if (shape.toShapeId() == putRecordsOutputShapeId) {
      val members =
        shape.getAllMembers().asScala.toList.map { case (memberName, member) =>
          if (memberName == "FailedRecordCount")
            MemberShape
              .builder()
              .target(nonNegativeIntegerObjectShape.getId())
              .id(member.getId())
              .build()
          else member
        }

      StructureShape
        .builder()
        .members(members.asJavaCollection)
        .id(putRecordsOutputShapeId)
        .traits(shape.getAllTraits().values())
        .build()
    } else shape

  def transform(context: TransformContext): Model = {
    val withMappedTraits =
      context
        .getTransformer()
        .mapTraits(context.getModel(), traitTransform)

    val newShapes = withMappedTraits
      .shapes()
      .toList()
      .asScala
      .toList :+ nonNegativeIntegerObjectShape

    val withNewShapes =
      context.getTransformer().replaceShapes(withMappedTraits, newShapes.asJava)

    context.getTransformer().mapShapes(withNewShapes, shapeTransform)
  }

}
