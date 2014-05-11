import sbt._
import sbt.Keys._

object LibNotifyBuild extends Build {

  val generateNativeHeaders = taskKey[Unit]("Generates native headers from the compiled classes (triggers compile if project not compiled)")
  val javahClasses = settingKey[Seq[String]]("Defines full qualified names of the classes, which will be passed to the javah")
  val javahOutputDirectory = settingKey[File]("Directory, where the javah generated files are placed")
  val compileNative = taskKey[File]("Executes compilation of the native code")
  val compileNativeLibs = settingKey[Seq[String]]("Names of external libraries which will be linked during native compilation")
  val compileNativeArtifactName = settingKey[String]("Name of the generated library file")

  lazy val root = Project(
    id = "sbt-libnotify-plugin",
    base = file(".")
  ).settings(libNotifySettings: _*)

  lazy val libNotifySettings: Seq[Setting[_]] = Seq(
    javahClasses := Seq("it.paperdragon.sbt.LibNotify$"),
    compileNativeLibs := Seq("glib-2.0", "libnotify"),
    javahOutputDirectory := baseDirectory.value / "src" / "main" / "native",
    compileNativeArtifactName := "libLibNotify.so",
    generateNativeHeaders := generateNativeHeadersTask.value,
    compileNative := compileNativeTask.value,
    (compile in Compile) := {
      val analysis = (compile in Compile).value
      val extensionLocation = compileNative.value
      analysis
    },
    products in Compile += (target in Compile).value / "native"
  )


  lazy val generateNativeHeadersTask = Def.task[Unit] {
    val fullClasspathInCompile = (fullClasspath in Compile).value.map(_.data.getAbsolutePath).mkString(java.io.File.pathSeparator)
    val outputDirectory = javahOutputDirectory.value.getAbsolutePath
    streams.value.log.info(s"Generated header files to $outputDirectory")
    ("javah" :: "-cp" :: fullClasspathInCompile :: "-d" :: outputDirectory :: javahClasses.value.mkString(" ") :: Nil).!
  } dependsOn (compile in Compile)

  lazy val compileNativeTask = Def.task {
    val log = streams.value.log
    val cFiles = (javahOutputDirectory.value ** "*.c").get.map(f => f.getAbsolutePath).mkString(" ")
    log.debug(s"Discovered C sources: $cFiles")
    val compilerFlags = compileNativeLibs.value.map { lib => s"`pkg-config --cflags $lib`"}.mkString(" ")
    log.debug(s"Generated compiler flags: $compilerFlags")
    val linkerOptions = compileNativeLibs.value.map { lib => s"`pkg-config --libs $lib`"}.mkString(" ")
    log.debug(s"Generated linked options: $linkerOptions")
    val jvmIncludes = standardJavaIncludes((javaHome in Compile).value.getOrElse(file(System.getProperty("java.home"))))
    log.debug(s"Using JVM includes: $jvmIncludes")
    val outputDir = (target in Compile).value / "native"
    IO.createDirectory(outputDir)
    val outputFile = outputDir / compileNativeArtifactName.value
    log.debug(s"Compilation output $outputFile")
    val command = s"gcc -Wall -shared -fPIC $jvmIncludes $compilerFlags $cFiles $linkerOptions -o ${outputFile.getAbsolutePath}"
    log.debug(s"Compilation command: $command")
    // this is a bit tricky as we want to evaluate `pkg-config` in bash
    val exitCode = ("sh" :: "-c" :: command :: Nil).!
    if (exitCode != 0) sys.error(s"Compilation failed. Exit code: $exitCode") else outputFile
  }

  // computes list of arguments to gcc containing standard java includes
  // in form of -I/somepath/
  private def standardJavaIncludes(javaHome: File): String =
    List("include", "../include", "include/linux", "../include/linux").map { loc =>
      "-I" + (javaHome / loc).getAbsolutePath
    }.mkString(" ")


}