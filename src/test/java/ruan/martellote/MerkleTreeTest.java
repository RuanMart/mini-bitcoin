package ruan.martellote;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ruan.martellote.crypto.MerkleTree;
import ruan.martellote.utils.HashUtils;

public class MerkleTreeTest {

    private static final HashUtils HU_INSTANCE = new HashUtils();

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] txidFromString(String s) {
        return HashUtils.sha256d(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Entradas inválidas: null, vazia, e txid de tamanho inválido")
    void testInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> MerkleTree.buildRoot(null));

        assertThrows(IllegalArgumentException.class, () -> MerkleTree.buildRoot(List.of()));

        byte[] bad = new byte[31];
        assertThrows(IllegalArgumentException.class, () -> MerkleTree.buildRoot(List.of(bad)));
    }

    @Test
    @DisplayName("Lista com 1 txid: root deve ser o próprio txid")
    void testSingleTxid() {
        byte[] tx1 = txidFromString("tx1");
        byte[] root = MerkleTree.buildRoot(List.of(tx1));
        assertNotNull(root);
        assertEquals(32, root.length);
        assertArrayEquals(tx1, root, "Com 1 tx, a root deve ser o próprio txid");

        System.out.println("[single] tx1=" + HashUtils.bytesToHex(tx1));
        System.out.println("[single] root=" + HashUtils.bytesToHex(root));
    }

    @Test
    @DisplayName("Lista com 2 txids: root = sha256d(tx1 || tx2)")
    void testTwoTxids() {
        byte[] tx1 = txidFromString("tx1");
        byte[] tx2 = txidFromString("tx2");

        byte[] expected = HashUtils.sha256d(concat(tx1, tx2));

        byte[] root = MerkleTree.buildRoot(List.of(tx1, tx2));
        assertNotNull(root);
        assertEquals(32, root.length);
        assertArrayEquals(expected, root, "Root deve ser sha256d(tx1||tx2)");

        byte[] rootSwapped = MerkleTree.buildRoot(List.of(tx2, tx1));
        assertFalse(Arrays.equals(root, rootSwapped), "Trocar a ordem deve mudar a root");

        System.out.println("[two] tx1=" + HashUtils.bytesToHex(tx1));
        System.out.println("[two] tx2=" + HashUtils.bytesToHex(tx2));
        System.out.println("[two] expected=" + HashUtils.bytesToHex(expected));
        System.out.println("[two] root=" + HashUtils.bytesToHex(root));
    }

    @Test
    @DisplayName("Lista com 3 txids: duplicar o último no primeiro nível")
    void testThreeTxids() {
        byte[] tx1 = txidFromString("tx1");
        byte[] tx2 = txidFromString("tx2");
        byte[] tx3 = txidFromString("tx3");

        byte[] h12 = HashUtils.sha256d(concat(tx1, tx2));
        byte[] h33 = HashUtils.sha256d(concat(tx3, tx3)); // duplicação do último
        
        byte[] expected = HashUtils.sha256d(concat(h12, h33));

        byte[] root = MerkleTree.buildRoot(List.of(tx1, tx2, tx3));
        assertNotNull(root);
        assertEquals(32, root.length);
        assertArrayEquals(expected, root, "Root com 3 elementos deve duplicar o último no nível ímpar");

        System.out.println("[three] tx1=" + HashUtils.bytesToHex(tx1));
        System.out.println("[three] tx2=" + HashUtils.bytesToHex(tx2));
        System.out.println("[three] tx3=" + HashUtils.bytesToHex(tx3));
        System.out.println("[three] h12=" + HashUtils.bytesToHex(h12));
        System.out.println("[three] h33=" + HashUtils.bytesToHex(h33));
        System.out.println("[three] expected=" + HashUtils.bytesToHex(expected));
        System.out.println("[three] root=" + HashUtils.bytesToHex(root));
    }

    @Test
    @DisplayName("Lista com 4 txids: pares (tx1||tx2) e (tx3||tx4)")
    void testFourTxids() {
        byte[] tx1 = txidFromString("tx1");
        byte[] tx2 = txidFromString("tx2");
        byte[] tx3 = txidFromString("tx3");
        byte[] tx4 = txidFromString("tx4");

        byte[] h12 = HashUtils.sha256d(concat(tx1, tx2));
        byte[] h34 = HashUtils.sha256d(concat(tx3, tx4));
        byte[] expected = HashUtils.sha256d(concat(h12, h34));

        byte[] root = MerkleTree.buildRoot(List.of(tx1, tx2, tx3, tx4));
        assertNotNull(root);
        assertEquals(32, root.length);
        assertArrayEquals(expected, root);

        System.out.println("[four] tx1=" + HashUtils.bytesToHex(tx1));
        System.out.println("[four] tx2=" + HashUtils.bytesToHex(tx2));
        System.out.println("[four] tx3=" + HashUtils.bytesToHex(tx3));
        System.out.println("[four] tx4=" + HashUtils.bytesToHex(tx4));
        System.out.println("[four] h12=" + HashUtils.bytesToHex(h12));
        System.out.println("[four] h34=" + HashUtils.bytesToHex(h34));
        System.out.println("[four] expected=" + HashUtils.bytesToHex(expected));
        System.out.println("[four] root=" + HashUtils.bytesToHex(root));
    }

    @Test
    @DisplayName("Determinismo: mesma entrada ? mesma root; sensível à ordem")
    void testDeterminismAndOrderSensitivity() {
        List<byte[]> txs = new ArrayList<>();
        txs.add(txidFromString("a"));
        txs.add(txidFromString("b"));
        txs.add(txidFromString("c"));
        txs.add(txidFromString("d"));
        txs.add(txidFromString("e"));

        byte[] r1 = MerkleTree.buildRoot(txs);
        byte[] r2 = MerkleTree.buildRoot(txs);
        assertArrayEquals(r1, r2, "Mesma entrada deve produzir mesma root");

        // Ordem diferente ? root diferente
        List<byte[]> shuffled = new ArrayList<>(txs);
        java.util.Collections.reverse(shuffled);
        byte[] rShuffled = MerkleTree.buildRoot(shuffled);
        assertFalse(Arrays.equals(r1, rShuffled), "Trocar a ordem deve alterar a root");

        System.out.println("[determinism] root1=" + HashUtils.bytesToHex(r1));
        System.out.println("[determinism] root2=" + HashUtils.bytesToHex(r2));
        System.out.println("[determinism] shuffledRoot=" + HashUtils.bytesToHex(rShuffled));
    }
}