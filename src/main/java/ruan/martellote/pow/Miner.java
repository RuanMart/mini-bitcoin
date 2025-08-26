package ruan.martellote.pow;

import ruan.martellote.core.BlockHeader;
import ruan.martellote.utils.HashUtils;

public class Miner {

    public static class MinerResult {
        public final boolean found;
        public final int nonce;
        public final byte[] hash;
        public final long attempts;
        public final long durationMillis;
        public final double hashesPerSecond;

        public MinerResult(boolean found, int nonce, byte[] hash, long attempts, long durationMillis) {
            this.found = found;
            this.nonce = nonce;
            this.hash = hash;
            this.attempts = attempts;
            this.durationMillis = durationMillis;
            this.hashesPerSecond = durationMillis > 0 ? (attempts * 1000.0) / durationMillis : Double.NaN;
        }
    }

    // Mineração simples sem limite (para dev). Você pode adicionar overloads com maxAttempts/deadline.
    public MinerResult mine(BlockHeader header) {
        if (header == null) {
            throw new IllegalArgumentException("header cannot be null");
        }

        // Pré-condições básicas
        byte[] prev = header.getPreviousHash();
        byte[] root = header.getMerkleRoot();
        if (prev == null || prev.length != 32) {
            throw new IllegalStateException("previousHash must be 32 bytes");
        }
        if (root == null || root.length != 32) {
            throw new IllegalStateException("merkleRoot must be 32 bytes");
        }
        if (header.getBits() < 1) {
            throw new IllegalStateException("bits must be >= 1");
        }

        long start = System.currentTimeMillis();
        long attempts = 0;

        // Reinicie o nonce a partir de 0 (opcional; comente se preferir continuar de onde está)
        header.setNonce(0);

        // Loop de busca
        for (int nonce = 0; nonce >= 0; nonce++) { // cobre 0..Integer.MAX_VALUE
            header.setNonce(nonce);
            byte[] hash = header.computeHash();
            attempts++;

            if (Difficulty.meetsDifficultyHexPrefix(hash, header.getBits())) {
                long end = System.currentTimeMillis();
                return new MinerResult(true, nonce, hash, attempts, end - start);
            }

             if ((attempts & ((1 << 20) - 1)) == 0) { // a cada ~1M tentativas
                 System.out.println("Tried: " + attempts + " H/s≈" + ((attempts * 1000.0) / (System.currentTimeMillis() - start)));
             }
        }

        long end = System.currentTimeMillis();
        // Se sair do loop (overflow do int), não encontrou
        return new MinerResult(false, -1, null, attempts, end - start);
    }

    // Overload com limite de tentativas e deadline opcional
    public MinerResult mine(BlockHeader header, long maxAttempts, long deadlineMillis) {
        if (header == null) {
            throw new IllegalArgumentException("header cannot be null");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be > 0");
        }
        long start = System.currentTimeMillis();
        long endDeadline = deadlineMillis > 0 ? start + deadlineMillis : Long.MAX_VALUE;
        long attempts = 0;

        int nonce = 0;
        while (attempts < maxAttempts && System.currentTimeMillis() < endDeadline) {
            header.setNonce(nonce);
            byte[] hash = header.computeHash();
            attempts++;

            if (Difficulty.meetsDifficultyHexPrefix(hash, header.getBits())) {
                long end = System.currentTimeMillis();
                return new MinerResult(true, nonce, hash, attempts, end - start);
            }

            nonce++;
            if (nonce < 0) { // overflow
                nonce = 0;
            }
        }

        long end = System.currentTimeMillis();
        return new MinerResult(false, -1, null, attempts, end - start);
    }
}