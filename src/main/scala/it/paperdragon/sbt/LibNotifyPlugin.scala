package it.paperdragon.sbt

import sbt._
import Keys._
import sbt.testing.Status
import it.paperdragon.sbt.LibNotifyTestsListener.FormatFunction

/**
 * @author lpiepiora
 */
object LibNotifyPlugin extends Plugin {

  /**
   * Setting key defining icon being used
   */
  val libNotifyPassedIcon = settingKey[String]("Icon shown when the tests pass")

  val libNotifyFailedIcon = settingKey[String]("Icon shown when the tests fail")

  val libNotifyErrorIcon = settingKey[String]("Icon shown when the tests are in error")

  val libNotifySummaryFormat = settingKey[FormatFunction]("The function converting status to the notification's message summary")

  val libNotifyBodyFormat = settingKey[FormatFunction]("The function converting status to the notification's message body")

  private val shutdownLibNotifyHook = new ExitHook {
    override def runBeforeExiting(): Unit = LibNotify.shutdown()
  }

  private val initializeLibNotifyHook = (s: State) => {
    LibNotify.initialize("sbt-libnotify-plugin")
    s.copy(exitHooks = s.exitHooks + shutdownLibNotifyHook)
  }

  // by default we import the settings for compile config
  lazy val sbtLibNotifySettings = inConfig(Compile)(baseSbtLibNotifySettings) ++ Seq(
    (onLoad in Global) := {
      val previous = (onLoad in Global).value
      initializeLibNotifyHook compose previous
    }
  )

  /**
   * the base settings to be reused with various configurations
   */
  lazy val baseSbtLibNotifySettings: Seq[Setting[_]] = Seq(
    libNotifyPassedIcon := "info",
    libNotifyFailedIcon := "error",
    libNotifyErrorIcon := "error",
    libNotifySummaryFormat := defaultSummaryFormat,
    libNotifyBodyFormat := defaultBodyFormat,
    testListeners += {
      LibNotifyTestsListener(
        libNotifyPassedIcon.value, libNotifyFailedIcon.value, libNotifyErrorIcon.value,
        libNotifySummaryFormat.value, libNotifyBodyFormat.value
      )
    }
  )

  /**
   * The default formatting function for the notification summary
   * @param finalResult the final test's result
   * @param details the details of the test execution
   * @return
   */
  private def defaultSummaryFormat(finalResult: TestResult.Value, details: Map[Status, Int]) = s"$finalResult"

  /**
   * The default formatting function for the notification body
   * @param details the test result
   * @return the string placed in the body of the notification
   */
  private def defaultBodyFormat(finalResult: TestResult.Value, details: Map[Status, Int]): String = {
    val totalTests = details.values.sum
    val succeeded = details.get(Status.Success).getOrElse(0)
    val failed = details.get(Status.Failure).getOrElse(0)
    val ignored = details.get(Status.Ignored).getOrElse(0)

    s"$totalTests TOTAL\n$succeeded SUCCEEEDED\n$failed FAILED\n$ignored IGNORED"
  }

}
