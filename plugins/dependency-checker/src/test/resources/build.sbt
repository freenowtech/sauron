name := "test"

scalaVersion := "2.12.10"

resolvers ++= Seq(
  Resolver.mavenLocal,
  "confluent" at "https://packages.confluent.io/maven/",
  "jitpack" at "https://jitpack.io",
  Resolver.sonatypeRepo("releases")
)

inThisBuild(
  List(
    organization := "com.freenow",
    version := "0.1.0",
    scalaVersion := "2.12.10",
    assemblyJarName in assembly := "test.jar"
  )
)

libraryDependencies ++= {
  sys.props += "packaging.type" -> "jar"
  Seq(
    "org.postgresql" % "postgresql" % "42.2.12",
    "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
    "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % "3.5.0",
    "org.scalatest" %% "scalatest" % "3.1.0" % Test
  )
}

fork in run := true

//plugin to use annotation macros like @JsonCodec
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf"              => MergeStrategy.concat
  case x                             => MergeStrategy.first
}