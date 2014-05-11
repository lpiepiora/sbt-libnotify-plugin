sbtPlugin := true

name := "sbt-libnotify-plugin"

organization := "it.paperdragon"

lazy val sbtLibNotifyPlugin = Project(
  id = "sbt-libnotify-plugin",
  base = file(".")
) dependsOn libnotify

lazy val libnotify = project in file("libnotify")