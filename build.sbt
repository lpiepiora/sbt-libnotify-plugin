import bintray.Keys._

bintrayPublishSettings

releaseSettings

sbtPlugin := true

name := "sbt-libnotify-plugin"

organization := "it.paperdragon"

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

publishMavenStyle := false

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := None

lazy val sbtLibNotifyPlugin = Project(
  id = "sbt-libnotify-plugin",
  base = file(".")
)
