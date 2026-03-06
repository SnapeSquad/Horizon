package com.horizon.launcher.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Утилита для шифрования данных с использованием HWID как части ключа
 */
public class CryptoHelper {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    
    /**
     * Генерирует ключ шифрования на основе HWID
     */
    private static SecretKey generateKey(String hwid) throws Exception {
        // Используем SHA-256 для создания ключа из HWID
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest((hwid + "HorizonLauncher2024").getBytes(StandardCharsets.UTF_8));
        
        // Обрезаем до 32 байт для AES-256
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, 32);
        
        return new SecretKeySpec(key, ALGORITHM);
    }
    
    /**
     * Шифрует данные с использованием HWID
     */
    public static String encrypt(String data, String hwid) throws Exception {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        SecretKey key = generateKey(hwid);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * Расшифровывает данные с использованием HWID
     */
    public static String decrypt(String encryptedData, String hwid) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        SecretKey key = generateKey(hwid);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
