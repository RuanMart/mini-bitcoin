package ruan.martellote.core;

import ruan.martellote.crypto.MerkleTree;
import ruan.martellote.pow.Difficulty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block {
    private BlockHeader header;
    private List<byte[]> transactions;

    public BlockHeader getHeader() {
        return header;
    }
    public void setHeader(BlockHeader header) {
        this.header = header;
    }
    public List<byte[]> getTransactions() {
        List<byte[]> clone = new ArrayList<>();
        for (byte[] tx : transactions) {
            if (tx == null || tx.length != 32) {
                throw new IllegalArgumentException("each transaction must be 32 bytes");
            }
            clone.add(tx.clone());
        }
        return List.copyOf(clone);
    }

    public void setTransactions(List<byte[]> transactions) {
        if (transactions == null ||transactions.isEmpty()) {
            throw new IllegalArgumentException("transactions cannot be empty or null");
        }
        List<byte[]> clone = new ArrayList<>();
        for (byte[] tx : transactions) {
            if (tx == null || tx.length != 32) {
                throw new IllegalArgumentException("each transaction must be 32 bytes");
            }
            clone.add(tx.clone());
        }
        this.transactions = List.copyOf(clone);
    }

    public void computeAndSetMerkleRoot() {
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalStateException("Cannot compute merkle root: no transactions");
        }
        if (header == null) {
            throw new IllegalStateException("Cannot compute merkle root: no header");
        }

        byte[] merkleRoot = MerkleTree.buildRoot(transactions);
        header.setMerkleRoot(merkleRoot);
    }

    public byte[] getHash() {
        return header.computeHash();
    }

    public boolean verify() {
        if (transactions == null || transactions.isEmpty()) {
            return false;
        }
        if (header == null) {
            return false;
        }

        byte[] merkleRoot = MerkleTree.buildRoot(transactions);

        if (merkleRoot.length != 32) {
            return false;
        }

        if (header.getPreviousHash().length != 32) {
            return false;
        }

        if (!Arrays.equals(header.getMerkleRoot(), merkleRoot)) {
            return false;
        }
        for (byte[] tx : transactions) {
            if (tx.length != 32) {
                return false;
            }
        }
        return true;
    }

    public boolean validatePow() {
         return Difficulty.meetsDifficultyHexPrefix(header.computeHash(), header.getBits());
    }
}