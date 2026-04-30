# Phase 1 Environment Acceptance Report

- Date: 2026-04-30 12:13:17 +08:00
- Repo: M:\AI\1work\视频项目\ScreenStream

## Environment
- JDK 17: D:\JAVA\jdk-17
- JDK 8 (sdkmanager only): D:\JAVA\jdk
- Android SDK root: D:\JAVA
- platform-tools: D:\JAVA\platform-tools
- build-tools 36.0.0: D:\JAVA\build-tools\36.0.0
- platform android-36: D:\JAVA\platforms\android-36

## Local Properties
```properties
sdk.dir=D\:\\JAVA
```

## Commands
### gradle_version
- Command: `.\gradlew.bat --no-daemon --console=plain -version`
- Exit code: 0
```text

------------------------------------------------------------
Gradle 9.4.1
------------------------------------------------------------

Build time:    2026-03-19 08:46:28 UTC
Revision:      2d6327017519d23b96af35865dc997fcb544fb40

Kotlin:        2.3.0
Groovy:        4.0.29
Ant:           Apache Ant(TM) version 1.10.15 compiled on August 25 2024
Launcher JVM:  17.0.18 (Eclipse Adoptium 17.0.18+8)
Daemon JVM:    Compatible with Java 17, any vendor, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)
OS:            Windows 10 10.0 amd64

```

### common_test
- Command: `.\gradlew.bat --no-daemon --console=plain :common:testDebugUnitTest`
- Exit code: 1
```text
> Task :common:preDebugUnitTestBuild UP-TO-DATE
> Task :common:processDebugNavigationResources
> Task :common:generateDebugResources
> Task :common:javaPreCompileDebug
> Task :common:javaPreCompileDebugUnitTest
> Task :common:packageDebugResources
> Task :common:parseDebugLocalResources
> Task :common:generateDebugRFile
> Task :common:generateDebugUnitTestStubRFile
> Task :common:compileDebugKotlin
> Task :common:processDebugJavaRes
> Task :common:compileDebugJavaWithJavac
> Task :common:bundleLibCompileToJarDebug
> Task :common:bundleLibRuntimeToJarDebug

> Task :common:compileDebugUnitTestKotlin FAILED
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:3:20 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:8:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:34:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:66:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:104:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:132:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/common/src/test/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinatorTest.kt:147:6 Unresolved reference 'Test'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':common:compileDebugUnitTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 5m
14 actionable tasks: 14 executed
Configuration cache entry stored.
```

### mjpeg_webview_test
- Command: `.\gradlew.bat --no-daemon --console=plain :mjpeg:testDebugUnitTest --tests info.dvkr.screenstream.mjpeg.internal.WebViewForegroundSessionTest`
- Exit code: 1
```text
> Task :mjpeg:generateDebugResources
> Task :mjpeg:javaPreCompileDebug
> Task :mjpeg:javaPreCompileDebugUnitTest
> Task :common:compileDebugKotlin UP-TO-DATE
> Task :common:processDebugJavaRes UP-TO-DATE
> Task :common:compileDebugJavaWithJavac UP-TO-DATE
> Task :common:bundleLibCompileToJarDebug UP-TO-DATE
> Task :common:bundleLibRuntimeToJarDebug UP-TO-DATE
> Task :mjpeg:packageDebugResources
> Task :mjpeg:parseDebugLocalResources
> Task :mjpeg:generateDebugRFile
> Task :mjpeg:generateDebugUnitTestStubRFile
> Task :mjpeg:compileDebugKotlin
> Task :mjpeg:compileDebugJavaWithJavac NO-SOURCE
> Task :mjpeg:processDebugJavaRes
> Task :mjpeg:bundleLibRuntimeToJarDebug
> Task :mjpeg:bundleLibCompileToJarDebug

> Task :mjpeg:compileDebugUnitTestKotlin FAILED
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/mjpeg/src/test/java/info/dvkr/screenstream/mjpeg/internal/WebViewForegroundSessionTest.kt:3:20 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/mjpeg/src/test/java/info/dvkr/screenstream/mjpeg/internal/WebViewForegroundSessionTest.kt:10:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/mjpeg/src/test/java/info/dvkr/screenstream/mjpeg/internal/WebViewForegroundSessionTest.kt:22:6 Unresolved reference 'Test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/mjpeg/src/test/java/info/dvkr/screenstream/mjpeg/internal/WebViewForegroundSessionTest.kt:35:6 Unresolved reference 'Test'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':mjpeg:compileDebugUnitTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 1m 21s
24 actionable tasks: 13 executed, 11 up-to-date
Configuration cache entry stored.
```

### app_compile_fdroid_kotlin
- Command: `.\gradlew.bat --no-daemon --console=plain :app:compileFDroidDebugKotlin`
- Exit code: 0
```text
> Task :common:generateDebugResources UP-TO-DATE
> Task :common:processDebugNavigationResources UP-TO-DATE
> Task :mjpeg:javaPreCompileDebug UP-TO-DATE
> Task :mjpeg:processDebugNavigationResources UP-TO-DATE
> Task :common:javaPreCompileDebug UP-TO-DATE
> Task :mjpeg:generateDebugResources UP-TO-DATE
> Task :common:packageDebugResources UP-TO-DATE
> Task :mjpeg:packageDebugResources UP-TO-DATE
> Task :common:parseDebugLocalResources UP-TO-DATE
> Task :mjpeg:parseDebugLocalResources UP-TO-DATE
> Task :common:generateDebugRFile UP-TO-DATE
> Task :mjpeg:generateDebugRFile UP-TO-DATE
> Task :app:generateFDroidDebugBuildConfig
> Task :app:mapFDroidDebugSourceSetPaths
> Task :app:generateFDroidDebugResources
> Task :mjpeg:extractDebugSupportedLocales
> Task :common:extractDebugSupportedLocales
> Task :app:extractFDroidDebugSupportedLocales
> Task :app:generateFDroidDebugLocaleConfig
> Task :app:packageFDroidDebugResources
> Task :common:compileDebugKotlin UP-TO-DATE
> Task :common:compileDebugJavaWithJavac UP-TO-DATE
> Task :common:bundleLibCompileToJarDebug UP-TO-DATE
> Task :app:processFDroidDebugNavigationResources
> Task :mjpeg:compileDebugKotlin UP-TO-DATE
> Task :app:parseFDroidDebugLocalResources
> Task :mjpeg:compileDebugJavaWithJavac NO-SOURCE
> Task :mjpeg:bundleLibCompileToJarDebug UP-TO-DATE
> Task :app:generateFDroidDebugRFile

> Task :app:compileFDroidDebugKotlin
w: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/main/java/info/dvkr/screenstream/SingleActivity.kt:207:18 This declaration overrides a deprecated member but is not marked as deprecated itself. Add the '@Deprecated' annotation or suppress the diagnostic.
w: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/main/java/info/dvkr/screenstream/SingleActivity.kt:213:19 'fun onBackPressed(): Unit' is deprecated. This method has been deprecated in favor of using the
      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.
      The OnBackPressedDispatcher controls how back button events are dispatched
      to one or more {@link OnBackPressedCallback} objects.

BUILD SUCCESSFUL in 36s
29 actionable tasks: 12 executed, 17 up-to-date
Configuration cache entry stored.
```

### app_compile_fdroid_androidtest_kotlin
- Command: `.\gradlew.bat --no-daemon --console=plain :app:compileFDroidDebugAndroidTestKotlin`
- Exit code: 1
```text
> Task :app:processFDroidDebugManifestForPackage
> Task :app:compileFDroidDebugJavaWithJavac
> Task :app:mergeFDroidDebugResources
> Task :app:processFDroidDebugResources
> Task :app:bundleFDroidDebugClassesToCompileJar

> Task :app:compileFDroidDebugAndroidTestKotlin FAILED
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:3:16 Unresolved reference 'test'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:10:49 Unresolved reference 'AndroidTestCase'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:16:9 Unresolved reference 'assertFalse'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:17:9 Unresolved reference 'assertFalse'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:20:9 Unresolved reference 'assertEquals'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:30:9 Unresolved reference 'assertEquals'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:50:9 Unresolved reference 'assertFalse'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:51:9 Unresolved reference 'assertEquals'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:52:9 Unresolved reference 'assertEquals'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:71:9 Unresolved reference 'assertTrue'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:72:9 Unresolved reference 'assertEquals'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:81:9 Unresolved reference 'assertTrue'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:88:9 Unresolved reference 'assertFalse'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:95:9 Unresolved reference 'assertTrue'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:96:9 Unresolved reference 'assertFalse'.
e: file:///M:/AI/1work/��Ƶ��Ŀ/ScreenStream/app/src/androidTest/java/info/dvkr/screenstream/SingleActivityMeetingSessionTest.kt:99:9 Unresolved reference 'assertFalse'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileFDroidDebugAndroidTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 25s
59 actionable tasks: 30 executed, 29 up-to-date
Configuration cache entry stored.
```

### app_test_fdroid
- Command: `.\gradlew.bat --no-daemon --console=plain :app:testFDroidDebugUnitTest`
- Exit code: 0
```text
> Task :mjpeg:parseDebugLocalResources UP-TO-DATE
> Task :common:parseDebugLocalResources UP-TO-DATE
> Task :common:generateDebugRFile UP-TO-DATE
> Task :mjpeg:generateDebugRFile UP-TO-DATE
> Task :common:compileDebugLibraryResources UP-TO-DATE
> Task :mjpeg:compileDebugLibraryResources UP-TO-DATE
> Task :app:checkFDroidDebugAarMetadata UP-TO-DATE
> Task :app:javaPreCompileFDroidDebugUnitTest
> Task :app:processFDroidDebugNavigationResources UP-TO-DATE
> Task :app:parseFDroidDebugLocalResources UP-TO-DATE
> Task :app:generateFDroidDebugRFile UP-TO-DATE
> Task :app:compileFDroidDebugNavigationResources UP-TO-DATE
> Task :app:mergeFDroidDebugResources UP-TO-DATE
> Task :app:processFDroidDebugMainManifest UP-TO-DATE
> Task :app:processFDroidDebugManifest UP-TO-DATE
> Task :app:processFDroidDebugManifestForPackage UP-TO-DATE
> Task :common:compileDebugKotlin UP-TO-DATE
> Task :common:processDebugJavaRes UP-TO-DATE
> Task :common:compileDebugJavaWithJavac UP-TO-DATE
> Task :common:bundleLibCompileToJarDebug UP-TO-DATE
> Task :common:bundleLibRuntimeToJarDebug UP-TO-DATE
> Task :app:processFDroidDebugResources UP-TO-DATE
> Task :mjpeg:compileDebugKotlin UP-TO-DATE
> Task :mjpeg:compileDebugJavaWithJavac NO-SOURCE
> Task :mjpeg:processDebugJavaRes UP-TO-DATE
> Task :mjpeg:bundleLibRuntimeToJarDebug UP-TO-DATE
> Task :mjpeg:bundleLibCompileToJarDebug UP-TO-DATE
> Task :app:compileFDroidDebugKotlin UP-TO-DATE
> Task :app:compileFDroidDebugJavaWithJavac UP-TO-DATE
> Task :app:bundleFDroidDebugClassesToCompileJar UP-TO-DATE
> Task :app:compileFDroidDebugUnitTestKotlin NO-SOURCE
> Task :app:processFDroidDebugUnitTestJavaRes NO-SOURCE
> Task :app:compileFDroidDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processFDroidDebugJavaRes
> Task :app:bundleFDroidDebugClassesToRuntimeJar
> Task :app:testFDroidDebugUnitTest NO-SOURCE

BUILD SUCCESSFUL in 12s
58 actionable tasks: 3 executed, 55 up-to-date
Configuration cache entry stored.
```

### app_assemble_fdroid
- Command: `.\gradlew.bat --no-daemon --console=plain :app:assembleFDroidDebug`
- Exit code: 0
```text
> Task :mjpeg:processDebugJavaRes UP-TO-DATE
> Task :mjpeg:mergeDebugJniLibFolders
> Task :common:mergeDebugJniLibFolders
> Task :app:processFDroidDebugMainManifest UP-TO-DATE
> Task :mjpeg:mergeDebugNativeLibs NO-SOURCE
> Task :app:processFDroidDebugManifest UP-TO-DATE
> Task :app:processFDroidDebugManifestForPackage UP-TO-DATE
> Task :common:mergeDebugNativeLibs NO-SOURCE
> Task :app:writeFDroidDebugAppMetadata
> Task :common:copyDebugJniLibsProjectOnly
> Task :mjpeg:copyDebugJniLibsProjectOnly
> Task :app:writeFDroidDebugSigningConfigVersions
> Task :app:compileFDroidDebugKotlin UP-TO-DATE
> Task :app:compileFDroidDebugJavaWithJavac UP-TO-DATE
> Task :mjpeg:mergeDebugAssets
> Task :app:processFDroidDebugJavaRes UP-TO-DATE
> Task :app:checkFDroidDebugDuplicateClasses
> Task :app:processFDroidDebugResources UP-TO-DATE
> Task :app:mergeFDroidDebugNativeLibs
> Task :app:mergeFDroidDebugAssets

> Task :app:stripFDroidDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so. Run with --info option to learn more.

> Task :app:compressFDroidDebugAssets
> Task :app:desugarFDroidDebugFileDependencies
> Task :app:dexBuilderFDroidDebug
> Task :app:mergeFDroidDebugJavaResource
> Task :app:l8DexDesugarLibFDroidDebug
> Task :app:mergeLibDexFDroidDebug
> Task :app:mergeProjectDexFDroidDebug
> Task :app:mergeFDroidDebugGlobalSynthetics
> Task :app:mergeExtDexFDroidDebug
> Task :app:packageFDroidDebug
> Task :app:assembleFDroidDebug
> Task :app:createFDroidDebugApkListingFileRedirect

BUILD SUCCESSFUL in 50s
80 actionable tasks: 25 executed, 55 up-to-date
Configuration cache entry stored.
```

## Git Status
```text

```

## Blocked: yes

## Notes
- `sdkmanager` required JDK 8; old Android tools in `D:\JAVA\tools` throw `javax.xml.bind` errors on JDK 17.
- Business code was not modified.
