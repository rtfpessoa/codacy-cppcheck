name := """codacy-cppcheck"""

version := "1.0.0-SNAPSHOT"

val languageVersion = "2.11.12"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.6",
  "com.codacy" %% "codacy-engine-scala-seed" % "2.7.8",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.github.pathikrit" %% "better-files" % "3.5.0"
)

mainClass in Compile := Some("codacy.Engine")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)
