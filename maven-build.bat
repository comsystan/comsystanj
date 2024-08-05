echo off

@REM This executes CreateCommandFiles.java to generate ContextCommand files from all the InteractiveCommand Csaj plugins
call mvn exec:java

@REM Usuall build
call mvn clean install
pause