import sbtbuildinfo.BuildInfoKey

object KCLFS2CirisSpecVars {

  val propsAndEnvs =
    KCLCirisSpecVars.propsAndEnvs ++ PropsAndEnvs(
      List(
        CirisUtil.propAndEnv(List("kcl", "fs2", "queue", "size"), "200"),
        CirisUtil
          .propAndEnv(List("kcl", "fs2", "commit", "max", "chunk"), "500"),
        CirisUtil
          .propAndEnv(List("kcl", "fs2", "commit", "max", "wait"), "5 seconds"),
        CirisUtil
          .propAndEnv(List("kcl", "fs2", "commit", "max", "retries"), "3"),
        CirisUtil
          .propAndEnv(
            List("kcl", "fs2", "commit", "retry", "interval"),
            "1 second"
          ),
        CirisUtil.propAndEnv(
          List("kcl", "processor", "auto", "commit"),
          "false"
        )
      )
    )

  val env: Map[String, String] = propsAndEnvs.envs
  val prop: Seq[String] = propsAndEnvs.props
  val buildInfoKeys: Seq[BuildInfoKey] = propsAndEnvs.buildInfoKeys
}
