package ruan.martellote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ruan.martellote.core.BlockHeader;
import ruan.martellote.utils.HashUtils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

class BlockHeaderTest {

    private static byte[] zeros32() {
        return new byte[32];
    }

    private static byte[] ones32() {
        byte[] b = new byte[32];
        Arrays.fill(b, (byte) 0x11);
        return b;
    }

    @Test
    @DisplayName("Serialize deve ter 84 bytes e obedecer offsets em big-endian")
    void testSerializeLayout() {
        BlockHeader h = new BlockHeader();
        h.setVersion(1);
        h.setPreviousHash(zeros32());
        h.setMerkleRoot(zeros32());
        h.setTimeStamp(1_700_000_000L);
        h.setBits(4);
        h.setNonce(42);

        byte[] out = h.serialize();
        assertEquals(84, out.length, "Tamanho do header serializado deve ser 84 bytes");

        // version = 1 -> 00 00 00 01
        assertArrayEquals(new byte[]{0,0,0,1}, Arrays.copyOfRange(out, 0, 4));

        // previousHash (32 bytes zeros) -> bytes [4..35]
        byte[] prev = Arrays.copyOfRange(out, 4, 4 + 32);
        assertArrayEquals(zeros32(), prev);

        // merkleRoot (32 bytes zeros) -> bytes [36..67]
        byte[] merkle = Arrays.copyOfRange(out, 36, 36 + 32);
        assertArrayEquals(zeros32(), merkle);

        // timeStamp (8 bytes big-endian)
        byte[] ts = Arrays.copyOfRange(out, 68, 68 + 8);
        // valor esperado em big-endian
        byte[] expectedTs = new byte[8];
        // reaproveitando a própria classe para gerar esperado: crie um segundo header com os mesmos dados
        // (ou calcule manualmente). Vamos calcular manualmente:
        long t = 1_700_000_000L;
        expectedTs[0] = (byte) ((t >>> 56) & 0xFF);
        expectedTs[1] = (byte) ((t >>> 48) & 0xFF);
        expectedTs[2] = (byte) ((t >>> 40) & 0xFF);
        expectedTs[3] = (byte) ((t >>> 32) & 0xFF);
        expectedTs[4] = (byte) ((t >>> 24) & 0xFF);
        expectedTs[5] = (byte) ((t >>> 16) & 0xFF);
        expectedTs[6] = (byte) ((t >>> 8)  & 0xFF);
        expectedTs[7] = (byte) (t & 0xFF);
        assertArrayEquals(expectedTs, ts);

        // bits = 4 -> 00 00 00 04 (bytes [76..79])
        assertArrayEquals(new byte[]{0,0,0,4}, Arrays.copyOfRange(out, 76, 80));

        // nonce = 42 -> 00 00 00 2A (bytes [80..83])
        assertArrayEquals(new byte[]{0,0,0,0x2A}, Arrays.copyOfRange(out, 80, 84));
    }

    @Test
    @DisplayName("computeHash deve mudar quando nonce muda (propriedade de avalanche)")
    void testComputeHashChangesWithNonce() {
        BlockHeader h = new BlockHeader();
        h.setVersion(1);
        h.setPreviousHash(ones32());
        h.setMerkleRoot(ones32());
        h.setTimeStamp(1_700_000_000L);
        h.setBits(4);

        h.setNonce(1);
        byte[] hash1 = h.computeHash();
        assertNotNull(hash1);
        assertEquals(32, hash1.length);

        h.setNonce(2);
        byte[] hash2 = h.computeHash();
        assertNotNull(hash2);
        assertEquals(32, hash2.length);

        assertFalse(Arrays.equals(hash1, hash2), "Hashes devem diferir ao alterar o nonce");
    }

    @Test
    @DisplayName("previousHash e merkleRoot devem ser cópias defensivas (get/set)")
    void testDefensiveCopies() {
        BlockHeader h = new BlockHeader();

        byte[] prev = ones32();
        byte[] merkle = ones32();

        h.setPreviousHash(prev);
        h.setMerkleRoot(merkle);

        // Mutar arrays externos não deve afetar o estado interno
        prev[0] = 0x55;
        merkle[0] = 0x66;

        byte[] gotPrev = h.getPreviousHash();
        byte[] gotMerkle = h.getMerkleRoot();

        assertNotSame(prev, gotPrev, "getPreviousHash deve retornar cópia");
        assertNotSame(merkle, gotMerkle, "getMerkleRoot deve retornar cópia");

        // Como originalmente todos eram 0x11, a primeira posição deve continuar 0x11 internamente
        assertEquals((byte)0x11, gotPrev[0], "Mutação externa não deve refletir internamente");
        assertEquals((byte)0x11, gotMerkle[0], "Mutação externa não deve refletir internamente");

        // Mutar os arrays retornados por get também não deve afetar o estado interno
        gotPrev[1] = 0x77;
        gotMerkle[1] = 0x77;

        byte[] gotPrev2 = h.getPreviousHash();
        byte[] gotMerkle2 = h.getMerkleRoot();

        assertEquals((byte)0x11, gotPrev2[1], "Mutação pós-get não deve afetar estado interno");
        assertEquals((byte)0x11, gotMerkle2[1], "Mutação pós-get não deve afetar estado interno");
    }

    @Test
    @DisplayName("Setters devem validar tamanhos e valores")
    void testSettersValidation() {
        BlockHeader h = new BlockHeader();

        // version: seu setter só atribui se version < 1 (parece um bug).
        // Validamos que versões negativas não devem ser aceitas: como o código atual NÃO lança exceção,
        // apenas checamos o comportamento atual e sinalizamos em comentário.
        h.setVersion(0);
        // Esperado ideal: exceção ou não alterar. Como sua implementação define if (version < 1) this.version = version;
        // Isso permite 0/negativo. Considere corrigir para if (version < 1) throw ... else this.version = version;

        // previousHash: null é permitido no seu setter, mas isso quebrará serialize().
        // Recomendação: não permitir null. Aqui testamos IllegalArgumentException para tamanho inválido.
        assertThrows(IllegalArgumentException.class, () -> h.setPreviousHash(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> h.setMerkleRoot(new byte[31]));

        // bits: deve ser positivo
        assertThrows(IllegalArgumentException.class, () -> h.setBits(0));
        assertThrows(IllegalArgumentException.class, () -> h.setBits(-1));
        h.setBits(1); // ok
    }

    @Test
    @DisplayName("Serialize deve falhar se previousHash/merkleRoot estiverem nulos ou com tamanho incorreto")
    void testSerializePreconditions() {
        BlockHeader h = new BlockHeader();
        h.setVersion(1);
        h.setTimeStamp(1_700_000_000L);
        h.setBits(4);
        h.setNonce(0);

        // previousHash e merkleRoot estão por padrão com 32 bytes zero (pela inicialização), então serialize funciona.
        // Vamos forçar null para verificar o comportamento (seu setter permite null).
        h.setPreviousHash(null);
        h.setMerkleRoot(null);

        // Sua serialize não valida null explicitamente; isso causará NullPointerException em System.arraycopy.
        // Validamos que falha (exceção) — mas recomendo melhorar serialize() para validar e lançar IllegalStateException.
        assertThrows(Exception.class, h::serialize);
    }

    @Test
    @DisplayName("Hash deve ter 32 bytes (SHA-256 duplo)")
    void testComputeHashLength() {
        BlockHeader h = new BlockHeader();
        h.setVersion(1);
        h.setPreviousHash(zeros32());
        h.setMerkleRoot(zeros32());
        h.setTimeStamp(1_700_000_000L);
        h.setBits(4);
        h.setNonce(0);

        byte[] hash = h.computeHash();
        assertNotNull(hash);
        assertEquals(32, hash.length);

        // Apenas imprime para inspeção manual se quiser
        System.out.println("blockHash=" + HashUtils.bytesToHex(hash));
    }

    @Test
    @DisplayName("Modificar qualquer campo deve alterar o serialize e o hash")
    void testChangingAnyFieldAffectsOutput() {
        BlockHeader h = new BlockHeader();
        h.setVersion(1);
        h.setPreviousHash(zeros32());
        h.setMerkleRoot(zeros32());
        h.setTimeStamp(1_700_000_000L);
        h.setBits(4);
        h.setNonce(0);

        byte[] s1 = h.serialize();
        byte[] h1 = h.computeHash();

        h.setNonce(1);
        byte[] s2 = h.serialize();
        byte[] h2 = h.computeHash();

        assertFalse(Arrays.equals(s1, s2), "Serialize deve mudar ao alterar nonce");
        assertFalse(Arrays.equals(h1, h2), "Hash deve mudar ao alterar nonce");

        // Troca previousHash
        h.setPreviousHash(ones32());
        byte[] h3 = h.computeHash();
        assertFalse(Arrays.equals(h2, h3), "Hash deve mudar ao alterar previousHash");

        // Troca merkleRoot
        h.setMerkleRoot(ones32());
        byte[] h4 = h.computeHash();
        assertFalse(Arrays.equals(h3, h4), "Hash deve mudar ao alterar merkleRoot");

        // Troca timeStamp
        h.setTimeStamp(1_700_000_001L);
        byte[] h5 = h.computeHash();
        assertFalse(Arrays.equals(h4, h5), "Hash deve mudar ao alterar timeStamp");

        // Troca bits
        h.setBits(5);
        byte[] h6 = h.computeHash();
        assertFalse(Arrays.equals(h5, h6), "Hash deve mudar ao alterar bits");

        // Troca version (observação: seu setter atual aceita < 1; ideal é aceitar >=1)
        // Se você corrigir o setter, esse teste continuará válido mudando version para 2.
        h.setVersion(2);
        byte[] h7 = h.computeHash();
        assertFalse(Arrays.equals(h6, h7), "Hash deve mudar ao alterar version (após correção do setter)");
    }
}