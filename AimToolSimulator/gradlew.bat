@ECHO OFF
REM Source-auditable lightweight wrapper; see tools\bootstrap\GradleWrapperMain.java
SET APP_HOME=%~dp0
java -cp "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
EXIT /b %ERRORLEVEL%
