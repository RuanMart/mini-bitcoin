# ? Mini Bitcoin (Java)

<p align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/4/46/Bitcoin.svg" alt="Mini Bitcoin Logo" width="120"/>
</p>

<p align="center">
  <b>Uma mini implementa��o educacional de uma blockchain inspirada no Bitcoin, escrita em Java.</b><br/>
  Hashing, �rvore de Merkle, Prova de Trabalho (PoW) e Minera��o estilo Bitcoin.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-3.8+-blue?logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-green" />
</p>

---

## Funcionalidades

- **Hashing**
    - SHA-256
    - Double SHA-256 (SHA-256d)
- **�rvore de Merkle** para c�lculo da Merkle Root
- **Blocos e Cabe�alhos**
    - `BlockHeader`: version, previousHash, merkleRoot, timeStamp, bits, nonce
    - Serializa��o determin�stica + double SHA-256
- **Prova de Trabalho (PoW)**
    - Dificuldade simples (zeros hex no prefixo)
    - Dificuldade estilo Bitcoin (`nBits` compacto)
- **Minera��o**
    - Busca de nonce simples
    - Modo "Bitcoin-like" com atualiza��o de timestamp
- **Blockchain in-memory**
    - Bloco g�nesis
    - Encadeamento e valida��o de blocos

---

## Estrutura do Projeto

```
mini-bitcoin-java/
|-- pom.xml
|-- src/
|   |-- main/java/ruan/martellote/
|   |   |-- crypto/        -> HashUtils, MerkleTree
|   |   |-- core/          -> BlockHeader, Block
|   |   |-- pow/           -> Difficulty, Miner
|   |   |-- chain/         -> Blockchain
|   |   Main
|   |-- resources/
|-- test/java/... (JUnit 5, opcional)
```

---

## Pr�-requisitos

- **Java 17+**
- **Maven 3.8+**
- CPU comum (minera��o � em CPU, **n�o usa GPU**)

---

## Como Rodar

### 1. Compilar
```bash
mvn -q -DskipTests=true clean package
```

### 2. Demo simples (zeros em hex)
Edite a dificuldade em `Main.java`:
```java
int difficultyHexZeros = 4;
```
Rode:
```bash
mvn -q exec:java -Dexec.mainClass="ruan.martellote.Main"
```

### 3. Demo estilo Bitcoin (`nBits`)
Edite em `Main.java`:
```java
final int N_BITS_GENESIS = 0x1D00FFFF;
final int N_BITS_BLOCKS  = 0x1C0FFFFF;
```
Rode:
```bash
mvn -q exec:java -Dexec.mainClass="ruan.martellote.Main"
```

---

## Exemplo de Sa�da

```
==== BLOCK (FOUND) ====
version   : 1
timeStamp : 1756183456
nBits     : 0x1F00FFFF
nonce     : 86141
prevHash  : 000053...
merkle    : 217cc2...
blockHash : 0000d219...
attempts  : 86142
duration  : 33 ms (? 2,61 MH/s)
```

---

## Ajustando a Dificuldade

- **Zeros em hex (simples)**
  ```java
  Difficulty.meetsDifficultyHexPrefix(hash, hexZeros);
  ```
    - `hexZeros = 4` ? hash deve come�ar com `"0000"`
    - Cada +1 nibble ? **16x mais dif�cil**

- **nBits (estilo Bitcoin)**
  ```java
  Difficulty.meetsDifficultyCompact(hash, nBits);
  ```
    - Genesis Bitcoin: `0x1D00FFFF` (impratic�vel em CPU Java)
    - Para simula��o:
        - F�cil: `0x1C0FFFFF`
        - M�dio: `0x1B0FFFFF`
        - Dif�cil: `0x1B007FFF`

---

## Testes

- **HashUtilsTest** ? valida SHA-256 e SHA-256d
- **MerkleTreeTest** ? compara Merkle Root conhecida

---

## Roadmap

- [ ] Transa��es reais com serializa��o pr�pria
- [ ] Ajuste autom�tico de dificuldade (retarget simplificado)
- [ ] Persist�ncia da blockchain em disco
- [ ] RPC leve via HTTP

---

## Licen�a

Este projeto � distribu�do sob a licen�a **MIT**.  
Sinta-se livre para usar, modificar e aprender com ele.

---

<p align="center">
  Feito por <b>Ruan Luis de Oliveira Martellote</b><br/>
  <i>"Estudar blockchain minerando conhecimento!"</i>
</p>
