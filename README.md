# Tema **ADV**, grupo **adv-03**

-----

## Índice

  [1. Constituição dos grupos e participação individual global](#1-constituição-dos-grupos-e-participação-individual-global)

  [2. Estrutura do repositório](#2-estrutura-do-repositório)

  [3. Relatório](#3-relatório)

  [4. Dependências](#4-dependências)

  [5. Como executar](#5-como-executar)

  [6. Objetivos](#6-objetivos)

  [7. Contribuições](#7-contribuições)

## 1. Constituição dos grupos e participação individual global

| NMec | Nome | email | Participação |
|:---:|:---|:---|:---:|
| 107849 | ALEXANDRE MIGUEL RODRIGUES COTOROBAI | <alexandrecotorobai@ua.pt> | 16.25% |
| 108073 | BERNARDO MIGUEL MADEIRA DE FIGUEIREDO | <bernardo.figueiredo@ua.pt> | 18.75% |
| 108215 | HUGO FRANCISCO DA COSTA CORREIA | <hf.correia@ua.pt> | 16.25% |
| 109089 | JOAQUIM VERTENTES ROSA | <joaquimvr15@ua.pt> | 16.25% |
| 108713 | LILIANA PAULA CRUZ RIBEIRO | <lilianapcribeiro@ua.pt> | 16.25% |
| 110056 | RICARDO MANUEL QUINTANEIRO ALMEIDA | <ricardoquintaneiro@ua.pt> | 16.25% |<>


## 2. Estrutura do repositório

- **src** - deve conter todo o código fonte do projeto.
- **doc** -- deve conter toda a documentação adicional a este README.

- **examples** -- deve conter os exemplos ilustrativos das linguagens criadas e os scripts.

  - Estes exemplos devem conter comentários (no formato aceite pelas linguagens),
      que os tornem auto-explicativos.


## 3. Relatório

Toda a informação adicional relacionada com este projeto pode ser encontrada no diretório [doc](doc/).



## 4. Dependências

Para executar o nosso programa é necessário ter instalado o python3 e o Java junto com as seguintes dependências instaladas:
  - numpy (pip install numpy)
  - opencv (pip install opencv-python)
  - antlr4 (sudo apt install antlr4)


## 5. Como executar

Para executar o nosso programa faça o seguinte procedimento:

Entre no diretório de exemplos:

```
cd c2023-adv-03/examples/
```

Mude as permissões dos scripts para executável:
```
chmod +x scriptname.sh
```

Execute o scrip "build" para compilar a linguagem:

```
./build
```

Compile o ficheiro desejado:

```
./compile <example(.adv)>
```

Execute o ficheiro desejado:
```
./run <example(.adv)>
```
Durante a execução do programa clique 'enter' para avançar para a próxima etapa.

Caso deseja limpar os ficheiros gerados, execute o script "clean":
```
./clean
```

## 6. Objetivos

Implementámos todos os objetivos propostos do Nível Mínimo. Dentro do Nível Desejável, foram implementados todos os pontos, retirando a construção gramatical da sequência de símbolos. Ainda dentro deste nível alguns dos outros pontos foram concluidos parcialmente, nomeadamente a instrução condicional, a instrução de repetição e as expressões booleanas onde apenas nos faltou implementar o compilador nas três.
Do nível Adicional conseguimos implementar a representação das setas das transições. 
Dos desafios, conseguimos também implementar a animação de um autómato e a leitura de uma string.

## 7. Contribuições

- Alexandre Cotorobai - Analisador Semântico, Compilador, StringTemplate;
- Bernardo Figueiredo - Gramática, Analisador Semântico, Compilador;
- Hugo Correia - StringTemplate, Relatório, Compilador;
- Joaquim Rosa - Analisador Semântico, Compilador, StringTemplate;
- Liliana Ribeiro - StringTemplate, Relatório, Classes em python;
- Ricardo Quintaneiro - Gramática, Classes em python, StringTemplate.
