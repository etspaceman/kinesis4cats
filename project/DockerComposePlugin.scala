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

  val dockerComposeUpBaseTask: Def.Initialize[Task[Unit]] = Def
    .task {
      val log = sbt.Keys.streams.value.log
      val cmd =
        s"docker compose -f ${composeFile.value} up -d "
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

  val dockerComposeUpTask: Def.Initialize[Task[Unit]] = Def.taskDyn {
    if (buildImage.value) {
      dockerComposeUpBaseTask.dependsOn(
        projectsToBuild.value.map(p => p / packageAndBuildDockerImage): _*
      )
    } else dockerComposeUpBaseTask
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

  val dockerComposeRestartTask: Def.Initialize[Task[Unit]] =
    Def.sequential(dockerComposeDownTask, dockerComposeUpTask)

  val dockerComposeLogsTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = sbt.Keys.streams.value.log
    val cmd =
      s"docker compose -f ${composeFile.value} logs"
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
    val cmd =
      s"docker compose -f ${composeFile.value} ps -a"
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

  def settings(
      build: Boolean,
      projects: List[Project]
  ): Seq[Setting[_]] =
    Seq(
      dockerComposeUp := dockerComposeUpTask.value,
      dockerComposeDown := dockerComposeDownTask.value,
      dockerComposeRestart := dockerComposeRestartTask.value,
      dockerComposeLogs := dockerComposeLogsTask.value,
      dockerComposePs := dockerComposePsTask.value,
      composeFileLocation := "docker/",
      composeFileName := s"docker-compose.yml",
      composeProjectName := sys.env
        .getOrElse("COMPOSE_PROJECT_NAME", "kinesis4cats"),
      buildImage := build,
      projectsToBuild := projects
    )
}

object DockerComposePluginKeys {
  val composeFileLocation =
    settingKey[String]("Path to docker compose files, e.g. docker/")
  val composeFileName =
    settingKey[String]("File name of the compose file, e.g. docker-compose.yml")
  val composeProjectName =
    settingKey[String]("Name of project for docker compose.")
  val buildImage = settingKey[Boolean](
    "Determines if dockerComposeUp should also build a docker image via the DockerImagePlugin"
  )
  val projectsToBuild = settingKey[Seq[Project]]("Projects to build images for")
  val dockerComposeTestQuick =
    taskKey[Unit]("Brings up docker, runs 'test', brings down docker")
  val dockerComposeUp =
    taskKey[Unit](
      "Builds the images and then runs `docker compose -f <file> up -d` for the scope"
    )
  val dockerComposeDown =
    taskKey[Unit]("Runs `docker compose -f <file> down` for the scope")
  val dockerComposeRestart =
    taskKey[Unit](
      "Runs `docker compose -f <file> down` and `docker compose -f <file> up` for the scope"
    )
  val dockerComposeLogs =
    taskKey[Unit]("Runs `docker compose -f <file> logs` for the scope")
  val dockerComposePs =
    taskKey[Unit]("Runs `docker compose -f <file> ps -a` for the scope")
}
