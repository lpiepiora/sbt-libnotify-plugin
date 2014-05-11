import bintray.Keys._

bintrayPublishSettings

sbtPlugin := true

name := "sbt-libnotify-plugin"

organization := "it.paperdragon"

version := "0.1.0"

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

publishMavenStyle := false

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := None

lazy val sbtLibNotifyPlugin = Project(
  id = "sbt-libnotify-plugin",
  base = file(".")
) dependsOn libnotify

lazy val libnotify = project in file("libnotify")
