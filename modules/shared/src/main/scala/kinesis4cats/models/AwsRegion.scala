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

package kinesis4cats.models

/** AWS Region enum. Can be used for both the v1 and v2 test kits
  *
  * @param name
  *   Name of region
  * @param awsArnPiece
  *   Prefix for ARN construction
  */
sealed abstract class AwsRegion(
    val name: String,
    val awsArnPiece: String
)

object AwsRegion {
  case object US_GOV_EAST_1 extends AwsRegion("us-gov-east-1", "aws-us-gov")
  case object US_GOV_WEST_1 extends AwsRegion("us-gov-west-1", "aws-us-gov")
  case object US_EAST_1 extends AwsRegion("us-east-1", "aws")
  case object US_EAST_2 extends AwsRegion("us-east-2", "aws")
  case object US_WEST_1 extends AwsRegion("us-west-1", "aws")
  case object US_WEST_2 extends AwsRegion("us-west-2", "aws")
  case object EU_WEST_1 extends AwsRegion("eu-west-1", "aws")
  case object EU_WEST_2 extends AwsRegion("eu-west-2", "aws")
  case object EU_WEST_3 extends AwsRegion("eu-west-3", "aws")
  case object EU_CENTRAL_1 extends AwsRegion("eu-central-1", "aws")
  case object EU_NORTH_1 extends AwsRegion("eu-north-1", "aws")
  case object EU_SOUTH_1 extends AwsRegion("eu-south-1", "aws")
  case object AP_EAST_1 extends AwsRegion("ap-east-1", "aws")
  case object AP_SOUTH_1 extends AwsRegion("ap-south-1", "aws")
  case object AP_SOUTHEAST_1 extends AwsRegion("ap-southeast-1", "aws")
  case object AP_SOUTHEAST_2 extends AwsRegion("ap-southeast-2", "aws")
  case object AP_NORTHEAST_1 extends AwsRegion("ap-northeast-1", "aws")
  case object AP_NORTHEAST_2 extends AwsRegion("ap-northeast-2", "aws")
  case object AP_NORTHEAST_3 extends AwsRegion("ap-northeast-3", "aws")
  case object SA_EAST_1 extends AwsRegion("sa-east-1", "aws")
  case object CN_NORTH_1 extends AwsRegion("cn-north-1", "aws-cn")
  case object CN_NORTHWEST_1 extends AwsRegion("cn-northwest-1", "aws-cn")
  case object CA_CENTRAL_1 extends AwsRegion("ca-central-1", "aws")
  case object ME_SOUTH_1 extends AwsRegion("me-south-1", "aws")
  case object AF_SOUTH_1 extends AwsRegion("af-south-1", "aws")
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
}
