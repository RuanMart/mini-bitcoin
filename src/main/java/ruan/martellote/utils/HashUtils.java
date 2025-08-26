package ruan.martellote.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {

    private static final ThreadLocal<MessageDigest> TL_MD = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    });

    public HashUtils() {}

    public static String toSha256Hex(String input) {
        MessageDigest md = TL_MD.get();
        md.reset();
        return bytesToHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String toSha256Hex(byte[] bytes) {
        MessageDigest md = TL_MD.get();
        md.reset();
        return bytesToHex(md.digest(bytes));
    }

    public static String sha256Hex(Path file) {
        MessageDigest md = TL_MD.get();
        try {
            md.reset();
            InputStream is = java.nio.file.Files.newInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            is.close();
            return bytesToHex(md.digest());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha256d(byte[] data) {
        MessageDigest md = TL_MD.get();
        md.reset();
        byte[] first = md.digest(data);
        md.reset();
        return md.digest(first);
    }

    public static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }


}