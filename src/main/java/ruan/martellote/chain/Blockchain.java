package ruan.martellote.chain;

import ruan.martellote.core.Block;
import ruan.martellote.core.BlockHeader;
import ruan.martellote.pow.Difficulty;
import ruan.martellote.pow.Miner;
import ruan.martellote.utils.HashUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Blockchain {

    private final List<Block> chain = new ArrayList<>();
    // Semântica atual: número de zeros hexadecimais no início do hash (Difficulty.meetsDifficultyHexPrefix)
    private final int difficultyHexZeros;
    private final Miner miner = new Miner();

    public Blockchain(int difficultyHexZeros) {
        if (difficultyHexZeros < 1) {
            throw new IllegalArgumentException("difficultyHexZeros must be >= 1");
        }
        this.difficultyHexZeros = difficultyHexZeros;
    }

    public int getDifficultyHexZeros() {
        return difficultyHexZeros;
    }

    public synchronized int getHeight() {
        return chain.size() - 1;
    }

    public synchronized Block getTip() {
        return chain.isEmpty() ? null : chain.get(chain.size() - 1);
    }

    public synchronized byte[] getTipHash() {
        Block tip = getTip();
        return tip == null ? null : tip.getHash().clone();
    }

    public synchronized List<Block> getBlocks() {
        // Retorna cópia imutável para evitar mutação externa
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    // ====================== Criação de blocos ======================

    public synchronized Block createGenesis(List<byte[]> txids) {
        if (txids == null || txids.isEmpty()) {
            throw new IllegalArgumentException("genesis txids cannot be null/empty");
        }
        if (!chain.isEmpty()) {
            throw new IllegalStateException("genesis already created");
        }

        BlockHeader header = new BlockHeader();
        header.setVersion(1);
        header.setPreviousHash(zero32());
        header.setTimeStamp(System.currentTimeMillis() / 1000L);
        header.setBits(difficultyHexZeros);
        header.setNonce(0);

        Block genesis = new Block();
        genesis.setHeader(header);
        genesis.setTransactions(txids);
        genesis.computeAndSetMerkleRoot();

        // Verificação pré-mineração
        if (!genesis.verify()) {
            throw new IllegalStateException("Genesis failed basic verification before mining");
        }

        // Minerar
        Miner.MinerResult res = miner.mine(header);
        if (!res.found) throw new IllegalStateException("Failed to mine genesis");

        // Sanidade pós-mineração: PoW e Merkle
        if (!Difficulty.meetsDifficultyHexPrefix(res.hash, header.getBits())) {
            throw new IllegalStateException("Genesis PoW invalid after mining");
        }
        if (!genesis.verify()) {
            throw new IllegalStateException("Genesis invalid after mining");
        }

        chain.add(genesis);
        return genesis;
    }

    public synchronized Block addBlock(List<byte[]> txids) {
        if (txids == null || txids.isEmpty()) {
            throw new IllegalArgumentException("txids cannot be null/empty");
        }
        if (chain.isEmpty()) {
            throw new IllegalStateException("create genesis first");
        }

        Block prev = getTip();
        byte[] prevHash = prev.getHash();

        BlockHeader header = new BlockHeader();
        header.setVersion(1);
        header.setPreviousHash(prevHash);
        header.setTimeStamp(System.currentTimeMillis() / 1000L);
        header.setBits(difficultyHexZeros);
        header.setNonce(0);

        Block block = new Block();
        block.setHeader(header);
        block.setTransactions(txids);
        block.computeAndSetMerkleRoot();

        if (!block.verify()) {
            throw new IllegalStateException("Block failed basic verification before mining");
        }

        Miner.MinerResult res = miner.mine(header);
        if (!res.found) throw new IllegalStateException("Failed to mine block");

        if (!Difficulty.meetsDifficultyHexPrefix(res.hash, header.getBits())) {
            throw new IllegalStateException("PoW invalid after mining");
        }
        if (!block.verify()) {
            throw new IllegalStateException("Block invalid after mining");
        }
        if (!Arrays.equals(header.getPreviousHash(), prevHash)) {
            throw new IllegalStateException("prevHash changed unexpectedly");
        }

        chain.add(block);
        return block;
    }

    // ====================== Validação da cadeia ======================

    public synchronized boolean validateChain() {
        if (chain.isEmpty()) return true;

        for (int i = 0; i < chain.size(); i++) {
            Block b = chain.get(i);
            BlockHeader h = b.getHeader();

            // 1) Verificação básica do bloco (Merkle + tamanhos dos txids)
            if (!b.verify()) return false;

            // 2) PoW
            byte[] hash = b.getHash();
            if (!Difficulty.meetsDifficultyHexPrefix(hash, h.getBits())) return false;

            // 3) Encadeamento
            if (i == 0) {
                // Gênesis: previousHash == 32 bytes zero
                if (!isZero32(h.getPreviousHash())) return false;
            } else {
                byte[] prevHash = chain.get(i - 1).getHash();
                if (!Arrays.equals(prevHash, h.getPreviousHash())) return false;
            }
        }
        return true;
    }

    // ====================== Utilidades ======================

    private static byte[] zero32() {
        return new byte[32];
    }

    private static boolean isZero32(byte[] a) {
        if (a == null || a.length != 32) return false;
        for (byte v : a) if (v != 0) return false;
        return true;
    }

    // Debug: imprime um sumário da cadeia
    public synchronized void printSummary() {
        System.out.println("Blockchain height: " + getHeight());
        for (int i = 0; i < chain.size(); i++) {
            Block b = chain.get(i);
            byte[] h = b.getHash();
            System.out.printf("#%d hash=%s prev=%s merkle=%s nonce=%d bits=%d%n",
                    i,
                    HashUtils.bytesToHex(h),
                    HashUtils.bytesToHex(b.getHeader().getPreviousHash()),
                    HashUtils.bytesToHex(b.getHeader().getMerkleRoot()),
                    b.getHeader().getNonce(),
                    b.getHeader().getBits()
            );
        }
    }
}