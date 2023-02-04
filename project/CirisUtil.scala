import sbtbuildinfo.BuildInfoKey

object CirisUtil {
  def propAndEnv(
      parts: List[String],
      value: String,
      propPrefix: String = "prop.",
      envPrefix: String = "ENV_",
      buildInfoKeyPrefix: Option[String] = None
  ): PropAndEnv = {
    val propVar = s"${propPrefix}${parts.map(_.toLowerCase()).mkString(".")}"
    val envVar = s"${envPrefix}${parts.map(_.toUpperCase()).mkString("_")}"
    val buildInfoKey = ((buildInfoKeyPrefix.toList ++ parts) match {
      case Nil          => Nil
      case head :: Nil  => List(head)
      case head :: tail => head +: tail.map(_.capitalize)
    }).mkString

    PropAndEnv(s"-D$propVar=$value", envVar -> value, buildInfoKey -> value)
  }
}

final case class PropsAndEnvs(
    values: List[PropAndEnv]
) {
  val props: List[String] = values.map(_.prop)
  val envs: Map[String, String] = values.map(_.env).toMap
  val buildInfoKeys: Seq[BuildInfoKey] = values.map(_.buildInfo).toSeq

  def ++(that: PropsAndEnvs) = copy(values ++ that.values)
}

final case class PropAndEnv(
    prop: String,
    env: (String, String),
    buildInfo: BuildInfoKey
)
