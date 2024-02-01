echo "Running... "

cd ../out

nome_arquivo="${1%.*}"
novo_nome_arquivo="$nome_arquivo.py"

python3 $novo_nome_arquivo

cd ../examples

echo "Done"