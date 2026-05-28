import scala.sys.process._

import sbt.Keys._
import sbt._

object DockerComposePlugin extends AutoPlugin {
  override def trigger = noTrigger
  override def requires: Plugins = DockerImagePlugin

  val autoImport: DockerComposePluginKeys.type = DockerComposePluginKeys
  import DockerImagePlugin.autoImport._
  import autoImport._

  val composeFile: Def.Initialize[Task[String]] =
    Def.task(s"${composeFileLocation.value}${composeFileName.value}")

  val dockerComposeUpBaseTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = sbt.Keys.streams.value.log
    val cmd = s"docker compose -f ${composeFile.value} up -d "
    log.info(s"Running $cmd")
    val res = Process(
      cmd,
      None,
      "DOCKER_TAG_VERSION" -> (ThisBuild / version).value,
      "COMPOSE_PROJECT_NAME" -> composeProjectName.value
    ).!
    if (res != 0)
      throw new IllegalStateException(s"docker compose up returned $res")
  }

  val dockerComposeKillTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = sbt.Keys.streams.value.log
    val cmd = s"docker compose -f ${composeFile.value} kill -s 9"
    log.info(s"Running $cmd")
    val res = Process(
      cmd,
      None,
      "DOCKER_TAG_VERSION" -> (ThisBuild / version).value,
      "COMPOSE_PROJECT_NAME" -> composeProjectName.value
    ).!
    if (res != 0)
      throw new IllegalStateException(s"docker compose kill returned $res")
  }

  val dockerComposeDownBaseTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = sbt.Keys.streams.value.log
    val cmd = s"docker compose -f ${composeFile.value} down -v"
    log.info(s"Running $cmd")
    val res = Process(
      cmd,
      None,
      "DOCKER_TAG_VERSION" -> (ThisBuild / version).value,
      "COMPOSE_PROJECT_NAME" -> composeProjectName.value
    ).!
    if (res != 0)
      throw new IllegalStateException(s"docker compose down returned $res")
  }

  val dockerComposeDownTask: Def.Initialize[Task[Unit]] =
    Def.sequential(
      dockerComposeKillTask,
      dockerComposeDownBaseTask
    )

  val dockerComposeLogsTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = sbt.Keys.streams.value.log
    val cmd = s"docker compose -f ${composeFile.value} logs"
    log.info(s"Running $cmd")
    val res = Process(
      cmd,
      None,
      "DOCKER_TAG_VERSION" -> (ThisBuild / version).value,
      "COMPOSE_PROJECT_NAME" -> composeProjectName.value
    ).!
    if (res != 0)
      throw new IllegalStateException(s"docker compose logs returned $res")
  }

  val dockerComposePsTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = sbt.Keys.streams.value.log
    val cmd = s"docker compose -f ${composeFile.value} ps -a"
    log.info(s"Running $cmd")
    val res = Process(
      cmd,
      None,
      "DOCKER_TAG_VERSION" -> (ThisBuild / version).value,
      "COMPOSE_PROJECT_NAME" -> composeProjectName.value
    ).!
    if (res != 0)
      throw new IllegalStateException(s"docker compose ps -a returned $res")
  }

  // A Command (not a Task) so it can issue `++ pinnedScala` before the
  // image build — packageAndBuildDockerImage's classpath resolves wrong
  // under a mismatched session scalaVersion.
  private def dockerComposeUpCommand(
      build: Boolean,
      projects: List[Project],
      pinnedScala: String
  ): Command =
    Command.command("dockerComposeUp") { state =>
      val buildCmds =
        if (build) projects.map(p => s"${p.id}/packageAndBuildDockerImage")
        else Nil
      val switch = if (build) List(s"++ $pinnedScala") else Nil
      (switch ++ buildCmds ++ List("dockerComposeUpBase")) ::: state
    }

  private val dockerComposeRestartCommand: Command =
    Command.command("dockerComposeRestart") { state =>
      List("dockerComposeDown", "dockerComposeUp") ::: state
    }

  def settings(
      build: Boolean,
      projects: List[Project],
      pinnedScala: String
  ): Seq[Setting[_]] =
    Seq(
      dockerComposeUpBase := dockerComposeUpBaseTask.value,
      dockerComposeDown := dockerComposeDownTask.value,
      dockerComposeLogs := dockerComposeLogsTask.value,
      dockerComposePs := dockerComposePsTask.value,
      composeFileLocation := "docker/",
      composeFileName := s"docker-compose.yml",
      composeProjectName := sys.env
        .getOrElse("COMPOSE_PROJECT_NAME", "kinesis4cats"),
      commands ++= Seq(
        dockerComposeUpCommand(build, projects, pinnedScala),
        dockerComposeRestartCommand
      )
    )
}

object DockerComposePluginKeys {
  val composeFileLocation =
    settingKey[String]("Path to docker compose files, e.g. docker/")
  val composeFileName =
    settingKey[String]("File name of the compose file, e.g. docker-compose.yml")
  val composeProjectName =
    settingKey[String]("Name of project for docker compose.")
  val dockerComposeTestQuick =
    taskKey[Unit]("Brings up docker, runs 'test', brings down docker")
  val dockerComposeUpBase =
    taskKey[Unit](
      "Runs `docker compose -f <file> up -d` without rebuilding images. Invoked by the dockerComposeUp command after image builds."
    )
  val dockerComposeDown =
    taskKey[Unit]("Runs `docker compose -f <file> down` for the scope")
  val dockerComposeLogs =
    taskKey[Unit]("Runs `docker compose -f <file> logs` for the scope")
  val dockerComposePs =
    taskKey[Unit]("Runs `docker compose -f <file> ps -a` for the scope")
}
