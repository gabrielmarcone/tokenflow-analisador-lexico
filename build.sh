#!/bin/sh
# Compila o projeto e gera o TokenFlow.jar.
#   ./build.sh          compila e gera o jar
#   ./build.sh teste    compila, gera o jar e roda todas as verificacoes
# Opcional: JAVAC_OPTS para flags extras do javac (ex.: --module-path do JavaFX
# quando o JDK nao traz JavaFX embutido).
set -e
cd "$(dirname "$0")"

JAVAC=javac
JAR=jar
JAVA=java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAR="$JAVA_HOME/bin/jar"
    JAVA="$JAVA_HOME/bin/java"
fi

rm -rf out
mkdir -p out
find src -name '*.java' > out/fontes.txt
"$JAVAC" $JAVAC_OPTS -encoding UTF-8 -d out @out/fontes.txt

cp src/minipascal/lexico/gui/*.fxml src/minipascal/lexico/gui/*.css src/minipascal/lexico/gui/*.png out/minipascal/lexico/gui/

printf 'Main-Class: minipascal.lexico.gui.MainApp\n' > out/manifest.txt
"$JAR" cfm TokenFlow.jar out/manifest.txt -C out minipascal

echo "TokenFlow.jar gerado."

if [ "$1" = "teste" ]; then
    for suite in Fase3 Fase4 Fase5 Fase6 Absurda Robustez Afd; do
        "$JAVA" -cp out "minipascal.lexico.verificacao.Verificacao$suite" > out/resultado.txt || { grep FALHA out/resultado.txt; echo "Verificacao$suite falhou."; exit 1; }
        grep "falha(s)" out/resultado.txt
    done
    echo "Todas as verificacoes passaram."
fi