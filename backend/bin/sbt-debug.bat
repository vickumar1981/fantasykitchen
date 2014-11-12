@echo off
set JAVA_OPTS=-XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:MaxPermSize=256m -Xmx512M -Xss2M
set SCRIPT_DIR=%~dp0
java %SBT_OPTS% %JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 -jar "%SCRIPT_DIR%sbt-launch.jar" %*

