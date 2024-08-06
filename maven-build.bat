echo off

@REM This executes CreateCommandFiles.java to generate ContextCommand files from all the InteractiveCommand Csaj plugins
call mvn exec:java

echo Part 1/2 finished
pause

@REM Usuall build
call mvn clean install
echo Part 2/2 finished
pause