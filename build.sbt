import com.typesafe.sbt.packager.docker.Cmd

name := "codacy-cppcheck"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-engine-scala-seed" % "3.1.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "com.github.pathikrit" %% "better-files" % "3.8.0"
)

mainClass in Compile := Some("codacy.Engine")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

mappings in Universal ++= {
  (resourceDirectory in Compile) map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "codacy-cppcheck-base:latest"

dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")

dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("WORKDIR", _) => Seq(Cmd("WORKDIR", "/opt/docker"))
  case cmd @ Cmd("ADD", _) =>
    Seq(Cmd("RUN", s"adduser -u 2004 -D $dockerUser"), cmd, Cmd("RUN", "mv /opt/docker/docs /docs"))
  case other => List(other)
}
