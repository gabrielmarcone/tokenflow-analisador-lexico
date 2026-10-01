@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

rem Compila o projeto e gera o TokenFlow.jar.
rem   .\build.bat          compila e gera o jar
rem   .\build.bat teste    compila, gera o jar e roda todas as verificacoes

set "JAVAC=javac"
set "JAR=jar"
set "JAVA=java"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAR=%JAVA_HOME%\bin\jar.exe"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
)
for /d %%D in ("%ProgramFiles%\Java\jdk1.8*") do (
    set "JAVAC=%%~D\bin\javac.exe"
    set "JAR=%%~D\bin\jar.exe"
    set "JAVA=%%~D\bin\java.exe"
)

echo Compilador em uso:
"%JAVAC%" -version
if errorlevel 1 goto erro

for %%E in (Main.fxml application.css icon.png) do (
    if not exist "src\minipascal\lexico\gui\%%E" (
        echo Arquivo faltando: src\minipascal\lexico\gui\%%E
        goto erro
    )
)

if exist out rmdir /s /q out
mkdir out

set "FONTES="
for /r src %%F in (*.java) do set FONTES=!FONTES! "%%F"
"%JAVAC%" %JAVAC_OPTS% -encoding UTF-8 -d out !FONTES!
if errorlevel 1 goto erro

for %%E in (Main.fxml application.css icon.png) do copy /y "src\minipascal\lexico\gui\%%E" "out\minipascal\lexico\gui\" >nul
if errorlevel 1 goto erro

echo Main-Class: minipascal.lexico.gui.MainApp> out\manifest.txt
"%JAR%" cfm TokenFlow.jar out\manifest.txt -C out minipascal
if errorlevel 1 goto erro

echo TokenFlow.jar gerado.

if /i "%~1"=="teste" (
    for %%S in (Fase3 Fase4 Fase5 Fase6 Absurda Robustez) do (
        "!JAVA!" -cp out minipascal.lexico.verificacao.Verificacao%%S > out\resultado.txt
        set "RC=!errorlevel!"
        findstr /c:"FALHA" /c:"falha(s)" out\resultado.txt
        if not "!RC!"=="0" (
            echo Verificacao%%S falhou.
            goto erro
        )
    )
    echo Todas as verificacoes passaram.
)

goto fim

:erro
echo.
echo A build falhou. Veja a mensagem acima.
endlocal
echo %cmdcmdline% | find /i "/c" >nul && pause
exit /b 1

:fim
endlocal
echo %cmdcmdline% | find /i "/c" >nul && pause