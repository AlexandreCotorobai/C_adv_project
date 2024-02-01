#!/bin/bash

#Usar o compilador da linguagem java para compilar o exemplo passado como argumento
set -e
echo "Compiling your example..."

#verificar se o número de argumentos é válido
if [ $# -ne 1 ]; then
    echo "Número de argumentos inválido"
    exit 1
fi

#verificar se o argumento é um ficheiro
if [ ! -f $1 ]; then
    echo "O argumento não é um ficheiro"
    exit 1
fi

#verficar se o ficheiro é um ficheiro .adv
if [ ${1: -4} != ".adv" ]; then
    echo "O ficheiro não é um ficheiro .adv"
    exit 1
fi

#verificar se o ficheiro existe
if [ ! -e $1 ]; then
    echo "O ficheiro não existe"
    exit 1
fi

nome_arquivo="${1%.*}"
novo_nome_arquivo="$nome_arquivo.py"

#Compilar o ficheiro passado como argumento (está na pasta examples)
# check if dir out exists
if [ ! -d "../out" ]; then
    mkdir ../out
fi

cd ../src
java advMain ../examples/$1 
mv $novo_nome_arquivo ../out/
cd ../examples
echo "Compilation complete"