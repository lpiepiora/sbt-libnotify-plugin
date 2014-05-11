package it.paperdragon.sbt

/**
 * Interface for libnotify
 *
 * = Usage Example =
 *
 * {{{
 *   LibNotify.initialize("my-app-name")
 *
 *   LibNotify.showNotification("Header", "Body of the notification")
 *
 *   LibNotify.shutdown()
 * }}}
 */
object LibNotify {

  /**
   * Error type
   */
  type Error = String

  private var isInitialized = false

  private val libNotifyTempPath = {
    val path = java.nio.file.Files.createTempFile(null, null)
    path.toFile.deleteOnExit()
    path
  }

  /**
   * Initializes the `LibNotify`. Normally it should be called at the application startup, before any calls to `showNotification`
   * @param appName the app name, passed to native function notify_init
   * @return returns true if initialized properly
   */
  def initialize(appName: String): Boolean = {
    import java.nio.file.StandardCopyOption._
    LibNotify.synchronized {
      if (isInitialized) true
      else {
        java.nio.file.Files.copy(getClass.getResourceAsStream("/libLibNotify.so"), libNotifyTempPath, REPLACE_EXISTING)
        System.load(libNotifyTempPath.toAbsolutePath.toString)

        // we have loaded the lib so we can actually now initialize the stuff
        isInitialized = notifyInitializeInternal(appName)

        // lets cleanup
        if (!isInitialized) libNotifyTempPath.toFile.delete()

        isInitialized
      }
    }
  }

  /**
   * Displays a notification message to the user. If a message is presently shown it's updated.
   * @param summary the header for the message
   * @param body the body for the message
   * @param icon the icon used for the notification
   * @return error message if failed to show the message
   */
  def showNotification(summary: String, body: String, icon: Option[String] = None): Option[Error] = {
    LibNotify.synchronized {
      ensureInitialized()
      val maybeError = showNotificationInternal(summary, body, icon.getOrElse(null))
      if (maybeError == null) None else Some(maybeError)
    }
  }

  /**
   * Shuts down the `LibNotify`. Normally this method should be called at the application shutdown.
   * After it is called no calls to `showNotification` are allowed.
   */
  def shutdown(): Unit = {
    if (isInitialized) {
      libNotifyTempPath.toFile.delete()
      notifyDestroyInternal()
      isInitialized = false
    }
  }

  /**
   * Calls `notify_init` via JNI
   * @param appName the application name
   * @return `true` if initialized successfully
   */
  @native protected[this] def notifyInitializeInternal(appName: String): Boolean

  /**
   * Creates or updates notification via JNI
   * @param summary the summary text
   * @param body the body text
   * @param icon the icon which will be used for the notification
   * @return error message or `null` if no error
   */
  @native protected[this] def showNotificationInternal(summary: String, body: String, icon: String): String

  /**
   * Calls `notify_uninit` via JNI
   */
  @native protected[this] def notifyDestroyInternal(): Unit

  /**
   * Checks if the library is initialized
   */
  private def ensureInitialized() =
    if (!isInitialized) throw new IllegalStateException("LibNotify is NOT initialized. Call initialize() first")

}
