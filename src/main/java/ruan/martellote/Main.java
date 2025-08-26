package ruan.martellote;

import ruan.martellote.core.Block;
import ruan.martellote.core.BlockHeader;
import ruan.martellote.pow.Difficulty;
import ruan.martellote.utils.HashUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simulação de mineração estilo Bitcoin:
 * - Dificuldade via nBits (compact target), não por zeros hex.
 * - Atualiza timestamp periodicamente durante o loop.
 * - Calcula hashpower aproximado e imprime progresso.
 */
public class Main {

    // ===== Configs da simulação =====
    // Alvo (compacto) bem fácil para CPU encontrar blocos periodicamente.
    // Ex.: 0x1f00ffff ~ alvo muito alto (difícil baixa); ajuste conforme sua máquina.
    private static final int N_BITS_GENESIS = 0x1D0FFFFF;
    private static final int N_BITS_BLOCKS  = 0x1D0FFFFF;

    // Atualizar timestamp no header a cada N tentativas (simula clock do miner)
    private static final long TIMESTAMP_UPDATE_EVERY = 100_000;

    // Log de progresso a cada N tentativas
    private static final long LOG_EVERY = 1_000_000;

    // Quantos blocos minerar após o gênesis (ajuste para rodar horas)
    private static final int NUM_BLOCKS_TO_MINE = 2100000;

    // ===== Helpers =====
    private static byte[] txid(String s) {
        return HashUtils.sha256d(s.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] zeros32() {
        return new byte[32];
    }

    private static Block buildBlock(byte[] prevHash, List<byte[]> txids, int version, int nBits) {
        BlockHeader h = new BlockHeader();
        h.setVersion(version);
        h.setPreviousHash(prevHash);
        h.setTimeStamp(System.currentTimeMillis() / 1000L);
        h.setBits(nBits); // aqui bits = nBits compact target
        h.setNonce(0);

        Block b = new Block();
        b.setHeader(h);
        b.setTransactions(txids);
        b.computeAndSetMerkleRoot();

        if (!b.verify()) {
            throw new IllegalStateException("Block verification failed before mining");
        }
        return b;
    }

    private static void printFound(String title, Block block, byte[] blockHash, long attempts, long ms) {
        System.out.println("==== " + title + " (FOUND) ====");
        System.out.println("version   : " + block.getHeader().getVersion());
        System.out.println("timeStamp : " + block.getHeader().getTimeStamp());
        System.out.printf ("nBits     : 0x%08X%n", block.getHeader().getBits());
        System.out.println("nonce     : " + block.getHeader().getNonce());
        System.out.println("prevHash  : " + HashUtils.bytesToHex(block.getHeader().getPreviousHash()));
        System.out.println("merkle    : " + HashUtils.bytesToHex(block.getHeader().getMerkleRoot()));
        System.out.println("blockHash : " + HashUtils.bytesToHex(blockHash));
        System.out.println("attempts  : " + attempts);
        double hps = ms > 0 ? (attempts * 1000.0) / ms : Double.NaN;
        System.out.printf("duration  : %d ms (≈ %.2f H/s)%n", ms, hps);
        System.out.println();
    }

    // Loop de mineração estilo Bitcoin, sem a classe Miner, para controlar timestamp e logs
    private static byte[] mineLikeBitcoin(Block block) {
        BlockHeader header = block.getHeader();
        if (header == null) throw new IllegalStateException("Header is null");
        if (header.getPreviousHash() == null || header.getPreviousHash().length != 32)
            throw new IllegalStateException("previousHash must be 32 bytes");
        if (header.getMerkleRoot() == null || header.getMerkleRoot().length != 32)
            throw new IllegalStateException("merkleRoot must be 32 bytes");

        final int nBits = header.getBits();
        long attempts = 0L;
        long start = System.currentTimeMillis();
        long lastLog = start;
        byte[] bestHash = null;

        // Inicia a partir do nonce atual (pode ser 0)
        int nonce = header.getNonce();
        long nextTsUpdate = TIMESTAMP_UPDATE_EVERY;

        while (true) {
            header.setNonce(nonce);

            // Atualiza timestamp periodicamente (segundos Unix)
            if (attempts >= nextTsUpdate) {
                header.setTimeStamp(System.currentTimeMillis() / 1000L);
                nextTsUpdate += TIMESTAMP_UPDATE_EVERY;
            }

            byte[] hash = header.computeHash();
            attempts++;

            // Guarda "melhor" hash (menor numericamente) para debug
            if (bestHash == null || compareUnsigned256(hash, bestHash) < 0) {
                bestHash = hash;
            }

            if (Difficulty.meetsDifficultyCompact(hash, nBits)) {
                long end = System.currentTimeMillis();
                printFound("BLOCK", block, hash, attempts, end - start);
                return hash;
            }

            // Logs periódicos de progresso
            if (attempts % LOG_EVERY == 0) {
                long now = System.currentTimeMillis();
                double hps = (attempts * 1000.0) / (now - start);
                System.out.printf("... tried %,d nonces | ~%.2f H/s | best=%s%n",
                        attempts, hps, HashUtils.bytesToHex(bestHash));
                lastLog = now;
            }

            // Próximo nonce; se overflowar, recomeça e deixa o timestamp seguir
            nonce++;
            if (nonce < 0) {
                nonce = 0;
                header.setTimeStamp(System.currentTimeMillis() / 1000L);
            }
        }
    }

    // Compara dois hashes como inteiros sem sinal de 256 bits (big-endian)
    private static int compareUnsigned256(byte[] a, byte[] b) {
        if (a == b) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        if (a.length != 32 || b.length != 32) throw new IllegalArgumentException("hash must be 32 bytes");
        for (int i = 0; i < 32; i++) {
            int va = a[i] & 0xFF;
            int vb = b[i] & 0xFF;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    public static void main(String[] args) {
        try {
            // ===== Gênesis =====
            List<byte[]> genesisTxs = new ArrayList<>();
            genesisTxs.add(txid("coinbase#0"));
            genesisTxs.add(txid("alice->bob:1.0"));

            Block genesis = buildBlock(zeros32(), genesisTxs, 1, N_BITS_GENESIS);
            byte[] genesisHash = mineLikeBitcoin(genesis);

            // Sanidade do PoW com compacto
            if (!Difficulty.meetsDifficultyCompact(genesisHash, genesis.getHeader().getBits())) {
                throw new IllegalStateException("Genesis PoW invalid");
            }
            if (!genesis.verify()) throw new IllegalStateException("Genesis Merkle invalid");

            // ===== Blocos subsequentes =====
            byte[] prevHash = genesisHash;
            for (int i = 1; i <= NUM_BLOCKS_TO_MINE; i++) {
                List<byte[]> txs = new ArrayList<>();
                txs.add(txid("coinbase#" + i));
                txs.add(txid("user" + i + "->user" + (i + 1) + ":" + (0.1 * i)));

                Block block = buildBlock(prevHash, txs, 1, N_BITS_BLOCKS);
                byte[] h = mineLikeBitcoin(block);

                if (!Difficulty.meetsDifficultyCompact(h, block.getHeader().getBits()))
                    throw new IllegalStateException("Block PoW invalid at height " + i);
                if (!block.verify())
                    throw new IllegalStateException("Block Merkle invalid at height " + i);

                // Encadeamento
                if (!Arrays.equals(block.getHeader().getPreviousHash(), prevHash))
                    throw new IllegalStateException("prevHash mismatch at height " + i);

                prevHash = h;
            }

            System.out.println("Simulação concluída. Deixe rodando para observar H/s e blocos encontrados ao longo do tempo.");
            System.out.println("Dica: ajuste N_BITS_* para calibrar a taxa de achados na sua máquina.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro na simulação: " + e.getMessage());
        }
    }
}