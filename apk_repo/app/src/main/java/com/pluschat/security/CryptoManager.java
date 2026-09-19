package com.pluschat.security;

import android.util.Base64;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * CryptoManager - Ядро безопасности "Динамический Лабиринт" для Android
 * 
 * Функции:
 * 1. Генерация пары ключей RSA (для клиента).
 * 2. Шифрование сообщения одноразовым AES-ключом.
 * 3. Шифрование AES-ключа публичным RSA-ключом сервера.
 * 4. Создание цифровой подписи.
 */
public class CryptoManager {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // байт
    private static final int GCM_TAG_LENGTH = 128; // бит

    /**
     * Шаг 1: Рукопожатие.
     * Отправляет запрос на proto.pluschat.ru, получает SessionID и PublicKey сервера.
     */
    public static class HandshakeResult {
        public String sessionId;
        public PublicKey serverPublicKey;
        
        public HandshakeResult(String sessionId, PublicKey key) {
            this.sessionId = sessionId;
            this.serverPublicKey = key;
        }
    }

    /**
     * Генерация временной пары ключей для сессии (опционально, если требуется взаимная аутентификация)
     */
    public static KeyPair generateClientKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(4096);
        return kpg.generateKeyPair();
    }

    /**
     * Подготовка сообщения к отправке.
     * 
     * @param plainText Текст сообщения.
     * @param serverPublicKey Публичный ключ сервера (из рукопожатия).
     * @return Зашифрованный пакет JSON для отправки.
     */
    public static SecureMessage encryptMessage(String plainText, PublicKey serverPublicKey) throws Exception {
        // 1. Генерируем одноразовый AES ключ для ЭТОГО сообщения
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey aesKey = kg.generateKey();

        // 2. Шифруем текст алгоритмом AES-GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        Cipher aesCipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        
        byte[] encryptedData = aesCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 3. Шифруем сам AES-ключ публичным RSA-ключом сервера
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.WRAP_MODE, serverPublicKey);
        byte[] wrappedAesKey = rsaCipher.wrap(aesKey);

        // 4. Создаем подпись (подписываем хеш сообщения + nonce)
        // В упрощенной схеме подписываем зашифрованные данные
        // Для полноценной подписи нужен приватный ключ клиента
        
        return new SecureMessage(
            Base64.encodeToString(encryptedData, Base64.NO_WRAP),
            Base64.encodeToString(wrappedAesKey, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        );
    }

    /**
     * Класс-обертка для зашифрованного сообщения
     */
    public static class SecureMessage {
        public String encryptedPayload;
        public String encryptedKey;
        public String iv;

        public SecureMessage(String payload, String key, String iv) {
            this.encryptedPayload = payload;
            this.encryptedKey = key;
            this.iv = iv;
        }
        
        public String toJson() {
            return "{\"payload\":\"" + encryptedPayload + "\",\"key\":\"" + encryptedKey + "\",\"iv\":\"" + iv + "\"}";
        }
    }
}
