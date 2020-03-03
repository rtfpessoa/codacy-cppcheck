import com.typesafe.sbt.packager.docker.Cmd

val commonSettings = Seq(scalaVersion := "2.13.1")

lazy val `doc-generator` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "com.github.pathikrit" %% "better-files" % "3.8.0",
      "com.typesafe.play" %% "play-json" % "2.8.1"
    )
  )

val dockerUser = "docker"

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "codacy-cppcheck",
    libraryDependencies ++= Seq("com.codacy" %% "codacy-engine-scala-seed" % "4.0.0"),
    mainClass in Compile := Some("codacy.Engine"),
    mappings in Universal ++= {
      (resourceDirectory in Compile) map { resourceDir: File =>
        def pathsMoved(dirSrc: File, dirDest: String, condition: File => Boolean) =
          for (path <- dirSrc.allPaths.get if condition(path))
            yield path -> path.toString.replaceFirst(dirSrc.toString, dirDest)

        pathsMoved(resourceDir / "docs", "/docs", path => !path.isDirectory) ++
          pathsMoved(resourceDir / "addons", "/addons", path => !path.isDirectory && path.name.startsWith("misra"))
      }
    }.value,
    daemonUser in Docker := dockerUser,
    daemonGroup in Docker := dockerUser,
    dockerBaseImage := "codacy-cppcheck-base:latest",
    dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}"),
    dockerCommands := dockerCommands.value.flatMap {
      case cmd @ Cmd("WORKDIR", _) => Seq(Cmd("WORKDIR", "/opt/docker"))
      case cmd @ Cmd("ADD", _) =>
        Seq(Cmd("RUN", s"adduser -u 2004 -D $dockerUser"), cmd, Cmd("RUN", "mv /opt/docker/docs /docs"))
      case other => List(other)
    }
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
