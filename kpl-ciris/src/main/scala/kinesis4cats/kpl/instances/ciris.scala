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

package kinesis4cats.kpl.instances

import scala.util.Try

import _root_.ciris._
import cats.syntax.all._
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration.ThreadingModel
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants.COMPRESSION
import com.amazonaws.services.schemaregistry.utils._
import software.amazon.awssdk.services.glue.model.Compatibility

object ciris {
  implicit val compressionConfigDecoder: ConfigDecoder[String, COMPRESSION] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(COMPRESSION.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as compression type: ${e.getMessage}"
        )
      )
    }

  implicit val regionsConfigDecoder: ConfigDecoder[String, Regions] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(Regions.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as region: ${e.getMessage}"
        )
      )
    }

  implicit val avroRecordTypeConfigDecoder
      : ConfigDecoder[String, AvroRecordType] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(AvroRecordType.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as avro record type: ${e.getMessage}"
        )
      )
    }

  implicit val protobufMessageTypeConfigDecoder
      : ConfigDecoder[String, ProtobufMessageType] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(ProtobufMessageType.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as protobuf message type: ${e.getMessage}"
        )
      )
    }

  implicit val compatibilityConfigDecoder
      : ConfigDecoder[String, Compatibility] = ConfigDecoder[String].mapEither {
    case (_, value) =>
      Try(Compatibility.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as compatability: ${e.getMessage}"
        )
      )
  }

  implicit val threadingModelConfigDecoder
      : ConfigDecoder[String, ThreadingModel] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(ThreadingModel.valueOf(value)).toEither.leftMap(e =>
        ConfigError(
          s"Could not parse $value as threading model: ${e.getMessage}"
        )
      )
    }

}
