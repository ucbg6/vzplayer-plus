@echo off

set java_exec=%~dp0%jdk-12.0.2/bin/java.exe
set vzjar=%~dp0%target

echo %java_exec%
echo %vzjar%

cd /D %vzjar%

"%java_exec%" -jar VZPlayer-1.0-SNAPSHOT-launcher.jar