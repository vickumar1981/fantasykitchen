@echo off
set JAVA_OPTS=-XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:MaxPermSize=256m -Xmx512M -Xss2M
set SCRIPT_DIR=%~dp0
java %SBT_OPTS% -Xmx256M %JAVA_OPTS% -jar "%SCRIPT_DIR%sbt-launch.jar" %*

