package ruan.martellote.core;

import ruan.martellote.utils.HashUtils;

public class BlockHeader {
    private int version;
    private byte[] previousHash = new byte[32];
    private byte[] merkleRoot = new byte[32];
    private long timeStamp;
    private int bits;
    private int nonce;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version >= 1) {
            this.version = version;
        }
    }

    public byte[] getPreviousHash() {
        return previousHash == null ? null : previousHash.clone();
    }

    public void setPreviousHash(byte[] previousHash) {
        if (previousHash == null) {
            this.previousHash = null;
            return;
        }
        if (previousHash.length != 32) {
            throw new IllegalArgumentException("previousHash must be 32 bytes");
        }
        this.previousHash = previousHash.clone();
    }

    public byte[] getMerkleRoot() {
        return merkleRoot == null ? null : merkleRoot.clone();
    }

    public void setMerkleRoot(byte[] merkleRoot) {
        if (merkleRoot == null) {
            this.merkleRoot = null;
            return;
        }
        if (merkleRoot.length != 32) {
            throw new IllegalArgumentException("merkleRoot must be 32 bytes");
        }
        this.merkleRoot = merkleRoot.clone();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public int getBits() {
        return bits;
    }

    public void setBits(int bits) {
        if (bits < 1) {
            throw new IllegalArgumentException("bits must be positive");
        }
        this.bits = bits;
    }

    public byte[] serialize() {
        byte[] out = new byte[84];
        int pos = 0;

        writeIntBE(version, out, pos); pos += 4;

        System.arraycopy(previousHash, 0, out, pos, 32); pos += 32;

        System.arraycopy(merkleRoot, 0, out, pos, 32); pos += 32;

        writeLongBE(timeStamp, out, pos); pos += 8;

        writeIntBE(bits, out, pos); pos += 4;

        writeIntBE(nonce, out, pos); pos += 4;

        if (pos != out.length) {
            throw new IllegalStateException("Serialization length mismatch: " + pos);
        }
        return out;
    }

    public byte[] computeHash() {
        byte[] serialized = serialize();
        return HashUtils.sha256d(serialized);
    }

    private static void writeIntBE(int v, byte[] out, int off) {
        out[off]     = (byte) ((v >>> 24) & 0xFF);
        out[off + 1] = (byte) ((v >>> 16) & 0xFF);
        out[off + 2] = (byte) ((v >>> 8)  & 0xFF);
        out[off + 3] = (byte) (v & 0xFF);
    }

    private static void writeLongBE(long v, byte[] out, int off) {
        out[off]     = (byte) ((v >>> 56) & 0xFF);
        out[off + 1] = (byte) ((v >>> 48) & 0xFF);
        out[off + 2] = (byte) ((v >>> 40) & 0xFF);
        out[off + 3] = (byte) ((v >>> 32) & 0xFF);
        out[off + 4] = (byte) ((v >>> 24) & 0xFF);
        out[off + 5] = (byte) ((v >>> 16) & 0xFF);
        out[off + 6] = (byte) ((v >>> 8)  & 0xFF);
        out[off + 7] = (byte) (v & 0xFF);
    }
}
