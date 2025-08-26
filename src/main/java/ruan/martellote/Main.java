package ruan.martellote;

import ruan.martellote.chain.Blockchain;
import ruan.martellote.core.Block;
import ruan.martellote.utils.HashUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Main {
    private static byte[] txid(String s) {
        return HashUtils.sha256d(s.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        try {
            int difficultyHexZeros = 4;
            Blockchain chain = new Blockchain(difficultyHexZeros);

            // Gênesis
            List<byte[]> genesisTxs = new ArrayList<>();
            genesisTxs.add(txid("coinbase#0"));
            genesisTxs.add(txid("alice->bob:1.0"));
            Block genesis = chain.createGenesis(genesisTxs);
            System.out.println("Genesis hash: " + HashUtils.bytesToHex(genesis.getHash()));

            // Bloco 1
            List<byte[]> txs1 = new ArrayList<>();
            txs1.add(txid("coinbase#1"));
            txs1.add(txid("bob->carol:0.4"));
            txs1.add(txid("carol->dave:0.2"));
            Block b1 = chain.addBlock(txs1);
            System.out.println("Block #1 hash: " + HashUtils.bytesToHex(b1.getHash()));

            boolean ok = chain.validateChain();
            System.out.println("Chain valid: " + ok + " | Height: " + chain.getHeight());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}