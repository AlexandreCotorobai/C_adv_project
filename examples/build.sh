#!/bin/bash
#How to compile: chmod +x build
#How to run this script: ./build.sh

echo "Building..."
set -e

# Compilar o compilador da linguagem java
cd ../src
antlr4-build
javac *.java
#Voltar para o diretório examples
cd ../examples
echo "Complete"