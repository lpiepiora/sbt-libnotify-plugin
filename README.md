sbt-libnotify-plugin
====================

SBT plugin for showing notifications about tests, designed to work with Ubuntu.

It uses native `libnotify` bindings via JNI to leverage Unity's specific behaviour of the notifications.
In particular the plugin will update existing notifications with new test results, because Ubuntu doesn't
allow for closing notifications.

Requirements
====================

This plugin requires:

 * [sbt](http://www.scala-sbt.org/) 0.13.x.

 * Installed the `libnotify4` package.

        sudo apt-get install libnotify-bin

Installation
===================

Per Project Basis
-------------------

In your `PROJECT_DIR/projects/plugins.sbt` use `addSbtPlugin("it.paperdragon" % "sbt-libnotify-plugin" % "0.5.0")`

You can use the plugin with `build.sbt` and `Build.scala` setup.

For using with the `build.sbt` add

    it.paperdragon.sbt.LibNotifyPlugin.sbtLibNotifySettings

to your `build.sbt`.

For using with the `Build.scala` add

    .settings(it.paperdragon.sbt.LibNotifyPlugin.sbtLibNotifySettings: _*)

to your `Build.scala`, for example

    object MyBuild extends Build {

        lazy val project = project.in(file("."))
            .settings(it.paperdragon.sbt.LibNotifyPlugin.sbtLibNotifySettings: _*)

    }

Globally
-------------------

Create `~/.sbt/0.13/plugins/build.sbt` if it doesn't exist and add

    addSbtPlugin("it.paperdragon" % "sbt-libnotify-plugin" % "0.5.0")

Create `~/.sbt/0.13/local.sbt` if it doesn't exist and add

    `it.paperdragon.sbt.LibNotifyPlugin.sbtLibNotifySettings`