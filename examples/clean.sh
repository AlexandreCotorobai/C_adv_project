#!/bin/bash

echo "Cleaning up ANTLR generated files..."

set -e
# Mudar para o diretório onde os arquivos ANTLR estão localizados
cd ../src

# Executar antlr4-clear para limpar os arquivos gerados
antlr4-clean

# Voltar ao diretório original
cd ..

echo "Clean up complete"