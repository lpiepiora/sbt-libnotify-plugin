package it.paperdragon.sbt.plugins.libnotify

import sbt.{TestEvent, TestResult, TestsListener}
import sbt.testing.Status
import it.paperdragon.libnotify.LibNotify
import it.paperdragon.sbt.plugins.libnotify.LibNotifyTestsListener.FormatFunction

/**
 *
 */
object LibNotifyTestsListener {

  /**
   * Type defining format function.
   */
  type FormatFunction = (TestResult.Value, Map[Status, Int]) => String

  /**
   * Creates new listener instance
   * @param passedIcon icon used for passed tests
   * @param failedIcon icon used for failed tests
   * @param errorIcon icon used for tests in error
   * @param summaryFormat the function which formats the summary message
   * @param bodyFormat the function, which formats the body message
   * @return
   */
  def apply(passedIcon: String, failedIcon: String, errorIcon: String, summaryFormat: FormatFunction, bodyFormat: FormatFunction) =
    new LibNotifyTestsListener(passedIcon, failedIcon, errorIcon, summaryFormat, bodyFormat)

}

/**
 * The test listener, which shows notifications when tests are ran.
 * @author lpiepiora
 */
class LibNotifyTestsListener(val passedIcon: String,
                             val failedIcon: String,
                             val errorIcon: String,
                             val summaryFormat: FormatFunction,
                             val bodyFormat: FormatFunction) extends TestsListener {

  /**
   * The test execution status
   */
  private var resultDetails: Map[Status, Int] = Map.empty

  /**
   * called once at the beginning
   */
  override def doInit(): Unit = {
    resultDetails = Map.empty
  }

  /**
   * called once, at end
   * @param finalResult the final test result
   */
  override def doComplete(finalResult: TestResult.Value): Unit = {

    val summary = summaryFormat(finalResult, resultDetails)
    val body = bodyFormat(finalResult, resultDetails)
    val icon = Some(getIcon(finalResult))

    LibNotify.showNotification(summary, body, icon)

  }

  /**
   * called for each test method or equivalent
   * @param event the test event
   */
  override def testEvent(event: TestEvent): Unit = {
    val eventsByType: Map[Status, Int] = event.detail.groupBy(_.status).mapValues(_.size)

    this.synchronized {
      resultDetails = eventsByType ++ resultDetails.map { case (k, v) => k -> (v + eventsByType.getOrElse(k, 0))}
    }

  }

  override def endGroup(name: String, t: Throwable): Unit = {}

  override def endGroup(name: String, result: TestResult.Value): Unit = {}

  override def startGroup(name: String): Unit = {}

  /**
   * Gets icon string for the specific error
   */
  private def getIcon(result: TestResult.Value): String = result match {
    case TestResult.Passed => passedIcon
    case TestResult.Failed => failedIcon
    case TestResult.Error => errorIcon
  }

}
