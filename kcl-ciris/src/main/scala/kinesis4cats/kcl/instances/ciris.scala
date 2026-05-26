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

package kinesis4cats.kcl.instances

import scala.util.Try

import _root_.ciris._
import cats.syntax.all._
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.Tag
import software.amazon.kinesis.common._
import software.amazon.kinesis.coordinator.CoordinatorConfig.ClientVersionConfig
import software.amazon.kinesis.coordinator.streamInfo.StreamIdOnboardingState
import software.amazon.kinesis.coordinator.streamInfo.StreamInfoMode
import software.amazon.kinesis.leases.LeaseAssignmentStrategy
import software.amazon.kinesis.metrics.MetricsLevel

import kinesis4cats.instances.ciris._

object ciris {
  implicit val billingModeConfigDecoder: ConfigDecoder[String, BillingMode] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(BillingMode.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as billing mode: ${e.getMessage}"
        )
      )
    }

  implicit val initialPositionConfigDecoder
      : ConfigDecoder[String, InitialPositionInStream] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(InitialPositionInStream.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not decode initial position $value: ${e.getMessage}"
        )
      )
    }

  implicit val initialPositionExtendedConfigDecoder
      : ConfigDecoder[String, InitialPositionInStreamExtended] =
    ConfigDecoder[String].mapEither { case (configKey, value) =>
      value.split(":").toList match {
        case position :: Nil =>
          initialPositionConfigDecoder
            .decode(configKey, position)
            .map(InitialPositionInStreamExtended.newInitialPosition)
        case position :: timestamp :: Nil =>
          initialPositionConfigDecoder.decode(configKey, position).flatMap {
            case InitialPositionInStream.AT_TIMESTAMP =>
              ConfigDecoder[String, java.util.Date]
                .decode(configKey, timestamp)
                .map { dt =>
                  InitialPositionInStreamExtended
                    .newInitialPositionAtTimestamp(dt)
                }
            case _ =>
              Left(
                ConfigError(s"Got 2 values for position $position, expected 1")
              )
          }
        case x =>
          Left(
            ConfigError(
              s"Could not parse $x into InitialPositionInStreamExtended. Must be either the InitialPositionInStream value, or AT_TIMESTAMP:some-timestamp"
            )
          )
      }
    }

  implicit val metricsLevelConfigDecoder: ConfigDecoder[String, MetricsLevel] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(MetricsLevel.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not decode metrics level $value: ${e.getMessage}"
        )
      )
    }

  implicit val leaseAssignmentStrategyConfigDecoder
      : ConfigDecoder[String, LeaseAssignmentStrategy] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(LeaseAssignmentStrategy.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not decode lease assignment strategy $value: ${e.getMessage}"
        )
      )
    }

  implicit val streamInfoModeConfigDecoder
      : ConfigDecoder[String, StreamInfoMode] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(StreamInfoMode.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not decode stream info mode $value: ${e.getMessage}"
        )
      )
    }

  implicit val clientVersionConfigDecoder
      : ConfigDecoder[String, ClientVersionConfig] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(ClientVersionConfig.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not decode client version config $value: ${e.getMessage}"
        )
      )
    }

  implicit val streamIdOnboardingStateConfigDecoder
      : ConfigDecoder[String, StreamIdOnboardingState] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(StreamIdOnboardingState.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not decode stream id onboarding state $value: ${e.getMessage}"
        )
      )
    }

  implicit val tagsConfigDecoder: ConfigDecoder[String, List[Tag]] =
    mapConfigDecoder[String, String].mapEither { case (_, value) =>
      value.toList.traverse { case (k, v) =>
        Try(
          Tag.builder().key(k.trim()).value(v.trim()).build()
        ).toEither.leftMap(e =>
          ConfigError(s"Could not construct Tag $value: ${e.getMessage}")
        )
      }
    }
}
