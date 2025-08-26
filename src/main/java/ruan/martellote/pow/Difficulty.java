package ruan.martellote.pow;

import java.math.BigInteger;

public class Difficulty {

    public static boolean meetsDifficultyHexPrefix(byte[] hash32, int hexZeros) {
        if (hash32 == null || hash32.length != 32) {
            throw new IllegalArgumentException("hash must be 32 bytes");
        }
        if (hexZeros < 1) return false;

        int fullZeroBytes = hexZeros / 2;
        boolean half = (hexZeros % 2) == 1;

        for (int i = 0; i < fullZeroBytes; i++) {
            if (hash32[i] != 0) return false;
        }

        if (half) {
            int next = hash32[fullZeroBytes] & 0xFF;
            if ((next >>> 4) != 0) return false;
        }

        return true;
    }

    /**
     * Converte nBits (formato compacto estilo Bitcoin) para o alvo (target) como BigInteger não assinado.
     *
     * nBits: [exponent (1 byte)][mantissa (3 bytes)] em big-endian.
     * target = mantissa * 256^(exponent - 3)
     *
     * Regras de validade:
     * - mantissa != 0
     * - bit de "negativo" (0x00800000) não deve estar setado
     * - exponent pode ser qualquer byte (0..255), mas alvos práticos ficam dentro de 0..0xFF.
     */
    public static BigInteger targetFromCompact(int nBits) {
        int exponent = (nBits >>> 24) & 0xFF;
        int mantissa = nBits & 0x007FFFFF; // ignora o bit de "negativo"
        boolean negative = (nBits & 0x00800000) != 0;

        if (negative) {
            throw new IllegalArgumentException("Invalid compact: negative bit set");
        }
        if (mantissa == 0) {
            throw new IllegalArgumentException("Invalid compact: mantissa is zero");
        }

        BigInteger mant = BigInteger.valueOf(mantissa);
        int power = exponent - 3;

        if (power >= 0) {
            return mant.shiftLeft(8 * power);
        } else {
            // Casos com exponent < 3: alvo é mantissa >> (8*(3-exponent))
            return mant.shiftRight(8 * (-power));
        }
    }

    /**
     * Verifica se o hash (interpretado como inteiro sem sinal de 256 bits, big-endian)
     * é menor ou igual ao target derivado de nBits.
     */
    public static boolean meetsDifficultyCompact(byte[] hash32, int nBits) {
        if (hash32 == null || hash32.length != 32) {
            throw new IllegalArgumentException("hash must be 32 bytes");
        }
        BigInteger target = targetFromCompact(nBits);
        if (target.signum() <= 0) {
            // Alvo deve ser positivo
            return false;
        }

        // Interpreta o hash como inteiro não assinado (big-endian)
        BigInteger h = new BigInteger(1, hash32);
        return h.compareTo(target) <= 0;
    }

    // ==== Helpers opcionais de debug ====

    /**
     * Converte o target BigInteger para um array de 32 bytes big-endian (sem sinal).
     */
    public static byte[] targetTo32Bytes(BigInteger target) {
        if (target == null || target.signum() < 0) {
            throw new IllegalArgumentException("target must be non-negative");
        }
        byte[] bytes = target.toByteArray(); // pode ter comprimento 33 com byte 0x00 de sinal
        if (bytes.length == 32) return bytes;
        if (bytes.length == 33 && bytes[0] == 0) {
            // remove o byte de sinal extra
            byte[] out = new byte[32];
            System.arraycopy(bytes, 1, out, 0, 32);
            return out;
        }
        if (bytes.length < 32) {
            byte[] out = new byte[32];
            // left pad com zeros
            System.arraycopy(bytes, 0, out, 32 - bytes.length, bytes.length);
            return out;
        }
        // bytes.length > 32 e não é o caso de 33 com 0 inicial: pega os 32 menos significativos
        byte[] out = new byte[32];
        System.arraycopy(bytes, bytes.length - 32, out, 0, 32);
        return out;
    }
}
