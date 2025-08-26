package ruan.martellote.crypto;

import ruan.martellote.utils.HashUtils;

import java.util.List;

public class MerkleTree {

    public static byte[] buildRoot(List<byte[]> txids) {
        if (txids == null || txids.isEmpty()) {
            throw new IllegalArgumentException("txids cannot be null or empty");
        }
        for (byte[] txid : txids) {
            if (txid == null || txid.length != 32) {
                throw new IllegalArgumentException("each txid must be 32 bytes");
            }
        }
        List<byte[]> level = new java.util.ArrayList<>(txids);
        while (level.size() > 1) {
            List<byte[]> next = new java.util.ArrayList<>((level.size() + 1) / 2);
            for (int i = 0; i < level.size(); i += 2) {
                byte[] left = level.get(i);
                byte[] right = (i + 1 < level.size()) ? level.get(i + 1) : left;

                byte[] cat = new byte[64];
                System.arraycopy(left, 0, cat, 0, 32);
                System.arraycopy(right, 0, cat, 32, 32);

                byte[] parent = HashUtils.sha256d(cat);
                next.add(parent);
            }
            level = next;
        }
        return level.get(0);
    }

  //  public String buildRootHex(List<String> txidsHex) {}

    public void verify() {

    }


}
