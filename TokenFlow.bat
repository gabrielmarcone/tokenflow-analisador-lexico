@echo off
setlocal
cd /d "%~dp0"

rem Abre o TokenFlow com o Java 8 (que traz o JavaFX embutido), mesmo que
rem outro Java esteja na frente no PATH.

set "JAVAW="
for /d %%D in ("%ProgramFiles%\Java\jdk1.8*") do if exist "%%~D\jre\lib\ext\jfxrt.jar" set "JAVAW=%%~D\bin\javaw.exe"
if not defined JAVAW for /d %%D in ("%ProgramFiles%\Java\jre1.8*") do if exist "%%~D\lib\ext\jfxrt.jar" set "JAVAW=%%~D\bin\javaw.exe"

if not defined JAVAW (
    echo Nao foi encontrado um Java 8 com JavaFX em "%ProgramFiles%\Java".
    echo Instale o JDK 8 ^(ex.: Oracle JDK 8u202^) ou rode: java -jar TokenFlow.jar
    pause
    exit /b 1
)

start "" "%JAVAW%" -jar TokenFlow.jar
