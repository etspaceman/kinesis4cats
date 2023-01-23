package kinesis4cats.ciris

import ciris._

object CirisReader {

  def readEnvString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] = {
    env(
      (prefix.toList ++ parts).map(_.toUpperCase()).mkString("_")
    )

  }

  def readPropString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] = prop(
    (prefix.toList ++ parts).map(_.toLowerCase()).mkString(".")
  )

  def readString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] =
    readEnvString(parts, prefix).or(readPropString(parts, prefix))

  def read[A](
      parts: List[String],
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, A] =
    readString(parts, prefix).as[A]

  def readDefaulted[A](
      parts: List[String],
      default: A,
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, A] =
    read(parts, prefix).default(default)

  def readOptional[A](
      parts: List[String],
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, Option[A]] =
    read(parts, prefix).option
}
