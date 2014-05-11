import sbt._
import sbt.Keys._
import Path.flat

object LibNotifyBuild extends Build {

  import LibNotifyBuildKeys._

  object LibNotifyBuildKeys {
    val generateNativeHeaders = taskKey[Unit]("Generates native headers from the compiled classes (triggers compile if project not compiled)")

    val javahClasses = settingKey[Seq[String]]("Defines full qualified names of the classes, which will be passed to the javah")

    val javahOutputDirectory = settingKey[File]("Directory, where the javah generated files are placed")

    val jvmHeaders = settingKey[Seq[File]]("Header provided by the JVM")

    val compileNative = taskKey[File]("Executes compilation of the native code")

    val compileNativeLibs = settingKey[Seq[String]]("Names of external libraries which will be linked during native compilation")

    val compileNativeArtifactName = settingKey[String]("Name of the generated library file")

    val nativeDirectory = settingKey[File]("Directory where the native sources are located")

    val nativeSources = taskKey[Seq[File]]("Returns all native source files")

    val nativeHeaders = taskKey[Seq[File]]("Returns all native header files")
  }

  lazy val root = Project(
    id = "sbt-libnotify-plugin",
    base = file(".")
  ).settings(libNotifySettings: _*)

  lazy val libNotifySettings: Seq[Setting[_]] = Seq(
    javahClasses := Seq("it.paperdragon.sbt.LibNotify$"),
    compileNativeLibs := Seq("glib-2.0", "libnotify"),
    nativeDirectory := baseDirectory.value / "src" / "main" / "native",
    javahOutputDirectory := nativeDirectory.value,
    jvmHeaders := standardJavaIncludes((javaHome in Compile).value.getOrElse(file(System.getProperty("java.home")))),
    compileNativeArtifactName := "libLibNotify.so",
    generateNativeHeaders := generateNativeHeadersTask.value,
    compileNative := compileNativeTask.value,
    (compile in Compile) := {
      val analysis = (compile in Compile).value
      val extensionLocation = compileNative.value
      analysis
    },
    products in Compile += (target in Compile).value / "native",
    nativeSources := nativeSourcesTask.value,
    nativeHeaders := nativeHeadersTask.value,
    (unmanagedSources in(Compile, packageSrc)) ++= nativeHeaders.value ++ nativeSources.value,
    (mappings in(Compile, packageSrc)) ++= (nativeHeaders.value ++ nativeSources.value) pair flat
  )

  lazy val nativeSourcesTask = Def.task[Seq[File]] {
    (nativeDirectory.value ** "*.c").get
  }

  lazy val nativeHeadersTask = Def.task[Seq[File]] {
    (nativeDirectory.value ** "*.h").get
  }

  lazy val generateNativeHeadersTask = Def.task[Unit] {
    val fullClasspathInCompile = (fullClasspath in Compile).value.files.mkString(java.io.File.pathSeparator)
    val outputDirectory = javahOutputDirectory.value.getAbsolutePath

    streams.value.log.info(s"Generated header files to $outputDirectory")

    ("javah" :: "-cp" :: fullClasspathInCompile :: "-d" :: outputDirectory :: javahClasses.value.mkString(" ") :: Nil).!

  } dependsOn (compile in Compile)

  lazy val compileNativeTask = Def.task {
    val log = streams.value.log

    val cFiles = nativeSources.value.mkString(" ")
    log.debug(s"Discovered C sources: $cFiles")

    val compilerFlags = compileNativeLibs.value.map { lib => s"`pkg-config --cflags $lib`"}.mkString(" ")
    log.debug(s"Generated compiler flags: $compilerFlags")

    val linkerOptions = compileNativeLibs.value.map { lib => s"`pkg-config --libs $lib`"}.mkString(" ")
    log.debug(s"Generated linked options: $linkerOptions")

    val jvmIncludes = jvmHeaders.value.map(header => s"-I ${header.getAbsolutePath}").mkString(" ")
    //
    log.debug(s"Using JVM includes: $jvmIncludes")

    val outputDir = (target in Compile).value / "native"
    IO.createDirectory(outputDir)

    val outputFile = outputDir / compileNativeArtifactName.value
    log.debug(s"Compilation output $outputFile")

    val command = s"gcc -Wall -shared -fPIC $jvmIncludes $compilerFlags $cFiles $linkerOptions -o $outputFile"
    log.debug(s"Compilation command: $command")

    // this is a bit tricky as we want to evaluate `pkg-config` in bash
    val exitCode = ("sh" :: "-c" :: command :: Nil).!
    if (exitCode != 0) sys.error(s"Compilation failed. Exit code: $exitCode") else outputFile
  }

  /**
   * Gets standard java includes for the given path
   * @param javaHome the java home
   * @return the includes which should be included in the native compilation process
   */
  private def standardJavaIncludes(javaHome: File): Seq[File] =
    List("include", "../include", "include/linux", "../include/linux").map(javaHome / _)

}