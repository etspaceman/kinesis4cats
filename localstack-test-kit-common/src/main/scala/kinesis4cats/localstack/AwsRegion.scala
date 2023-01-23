package kinesis4cats.localstack

import ciris.ConfigDecoder
import cats.syntax.all._

sealed abstract class AwsRegion(
    val name: String,
    val awsArnPiece: String = "aws"
)

object AwsRegion {
  case object US_GOV_EAST_1 extends AwsRegion("us-gov-east-1", "aws-us-gov")
  case object US_GOV_WEST_1 extends AwsRegion("us-gov-west-1", "aws-us-gov")
  case object US_EAST_1 extends AwsRegion("us-east-1")
  case object US_EAST_2 extends AwsRegion("us-east-2")
  case object US_WEST_1 extends AwsRegion("us-west-1")
  case object US_WEST_2 extends AwsRegion("us-west-2")
  case object EU_WEST_1 extends AwsRegion("eu-west-1")
  case object EU_WEST_2 extends AwsRegion("eu-west-2")
  case object EU_WEST_3 extends AwsRegion("eu-west-3")
  case object EU_CENTRAL_1 extends AwsRegion("eu-central-1")
  case object EU_NORTH_1 extends AwsRegion("eu-north-1")
  case object EU_SOUTH_1 extends AwsRegion("eu-south-1")
  case object AP_EAST_1 extends AwsRegion("ap-east-1")
  case object AP_SOUTH_1 extends AwsRegion("ap-south-1")
  case object AP_SOUTHEAST_1 extends AwsRegion("ap-southeast-1")
  case object AP_SOUTHEAST_2 extends AwsRegion("ap-southeast-2")
  case object AP_NORTHEAST_1 extends AwsRegion("ap-northeast-1")
  case object AP_NORTHEAST_2 extends AwsRegion("ap-northeast-2")
  case object AP_NORTHEAST_3 extends AwsRegion("ap-northeast-3")
  case object SA_EAST_1 extends AwsRegion("sa-east-1")
  case object CN_NORTH_1 extends AwsRegion("cn-north-1", "aws-cn")
  case object CN_NORTHWEST_1 extends AwsRegion("cn-northwest-1", "aws-cn")
  case object CA_CENTRAL_1 extends AwsRegion("ca-central-1")
  case object ME_SOUTH_1 extends AwsRegion("me-south-1")
  case object AF_SOUTH_1 extends AwsRegion("af-south-1")
  case object US_ISO_EAST_1 extends AwsRegion("us-iso-east-1", "aws-iso")
  case object US_ISOB_EAST_1 extends AwsRegion("us-isob-east-1", "aws-iso-b")
  case object US_ISO_WEST_1 extends AwsRegion("us-iso-west-1", "aws-iso")

  val values: List[AwsRegion] = List(
    US_GOV_EAST_1,
    US_GOV_WEST_1,
    US_EAST_1,
    US_EAST_2,
    US_WEST_1,
    US_WEST_2,
    EU_WEST_1,
    EU_WEST_2,
    EU_WEST_3,
    EU_CENTRAL_1,
    EU_NORTH_1,
    EU_SOUTH_1,
    AP_EAST_1,
    AP_SOUTH_1,
    AP_SOUTHEAST_1,
    AP_SOUTHEAST_2,
    AP_NORTHEAST_1,
    AP_NORTHEAST_2,
    AP_NORTHEAST_3,
    SA_EAST_1,
    CN_NORTH_1,
    CN_NORTHWEST_1,
    CA_CENTRAL_1,
    ME_SOUTH_1,
    AF_SOUTH_1,
    US_ISO_EAST_1,
    US_ISOB_EAST_1,
    US_ISO_WEST_1
  )

  implicit val awsRegionConfigDecoder: ConfigDecoder[String, AwsRegion] =
    ConfigDecoder[String, String].mapOption("AwsRegion")(x =>
      values.find(_.name === x)
    )
}
