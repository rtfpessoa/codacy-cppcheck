import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """codacy-cppcheck"""

version := "1.0.0-SNAPSHOT"

val languageVersion = "2.11.8"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.6",
  "com.codacy" %% "codacy-engine-scala-seed" % "2.6.33"
)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

version in Docker := "1.0.0"

organization := "com.codacy"

val cppcheckVersion = "1.77"

val installAll =
  s"""apk update --no-cache
      |&& apk add --no-cache bash
      |&& apk add --no-cache -t .required_apks wget make g++ pcre-dev
      |&& wget --no-check-certificate -O /tmp/cppcheck.tar.gz https://github.com/danmar/cppcheck/archive/$cppcheckVersion.tar.gz
      |&& tar -zxf /tmp/cppcheck.tar.gz -C /tmp
      |&& cd /tmp/cppcheck-$cppcheckVersion
      |&& make install CFGDIR=/cfg HAVE_RULES=yes CXXFLAGS="-O2 -DNDEBUG -Wall -Wno-sign-compare -Wno-unused-function --static"
      |&& apk del .required_apks
      |&& rm -rf /tmp/*
      |&& rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")

mappings in Universal <++= (resourceDirectory in Compile) map { (resourceDir: File) =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  for {
    path <- (src ***).get
    if !path.isDirectory
  } yield path -> path.toString.replaceFirst(src.toString, dest)
}

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "develar/java"

dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("WORKDIR", _) => List(cmd,
    Cmd("RUN", installAll)
  )
  case cmd@(Cmd("ADD", "opt /opt")) => List(cmd,
    Cmd("RUN", "mv /opt/docker/docs /docs"),
    Cmd("RUN", s"adduser -u 2004 -D $dockerUser"),
    ExecCmd("RUN", Seq("chown", "-R", s"$dockerUser:$dockerGroup", "/docs"): _*)
  )
  case other => List(other)
}
