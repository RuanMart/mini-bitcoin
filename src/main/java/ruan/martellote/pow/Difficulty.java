package ruan.martellote.pow;

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
}
