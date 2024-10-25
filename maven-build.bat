echo off

@REM This executes CreateCommandFiles.java to generate ContextCommand files from all the InteractiveCommand Csaj plugins
@REM call mvn exec:java
@REM echo Part 1/2 finished
@REM pause

@REM Usual build
call mvn clean install
@REM echo Part 2/2 finished
pause