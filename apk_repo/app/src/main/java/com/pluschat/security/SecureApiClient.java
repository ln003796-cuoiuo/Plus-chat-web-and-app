package com.pluschat.security;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import android.util.Base64;

/**
 * SecureApiClient - Клиент для связи с ПРОТО.ПЛЮСЧАТ.РФ
 * 
 * Отвечает за:
 * 1. Выполнение рукопожатия (Handshake).
 * 2. Отправку зашифрованных сообщений.
 * 3. Обработку ответов сервера.
 */
public class SecureApiClient {

    private static final String BASE_URL = "https://прото.плюсчат.рф"; // Или xn--n1aabel.xn--80avljg2a1c.xn--p1ai
    private static final String TAG = "SecureApiClient";

    private String currentSessionId = null;
    private PublicKey serverPublicKey = null;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Шаг 1: Рукопожатие с сервером.
     * Должно вызываться перед отправкой первого сообщения и после каждого ответа "request_new_handshake".
     */
    public void handshake(final Callback<HandshakeResult> callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/?act=handshake");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    if ("ok".equals(json.getString("status"))) {
                        currentSessionId = json.getString("session_id");
                        
                        // Декодируем публичный ключ из Base64
                        String pubKeyStr = json.getString("public_key");
                        byte[] pubKeyBytes = Base64.decode(pubKeyStr, Base64.NO_WRAP);
                        
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
                        serverPublicKey = keyFactory.generatePublic(keySpec);

                        Log.d(TAG, "Handshake successful. Session: " + currentSessionId);
                        callback.onSuccess(new HandshakeResult(currentSessionId, serverPublicKey));
                    } else {
                        throw new Exception("Handshake failed: " + json.toString());
                    }
                } else {
                    throw new Exception("HTTP Error: " + code);
                }
            } catch (Exception e) {
                Log.e(TAG, "Handshake error", e);
                callback.onError(e);
            }
        }).start();
    }

    /**
     * Шаг 2: Отправка зашифрованного сообщения.
     */
    public void sendSecureMessage(String plainText, final Callback<String> callback) {
        if (serverPublicKey == null || currentSessionId == null) {
            callback.onError(new Exception("Handshake required before sending"));
            return;
        }

        new Thread(() -> {
            try {
                // Шифруем сообщение локально
                CryptoManager.SecureMessage secureMsg = CryptoManager.encryptMessage(plainText, serverPublicKey);

                // Формируем JSON для отправки
                JSONObject payload = new JSONObject();
                payload.put("session_id", currentSessionId);
                payload.put("payload", secureMsg.encryptedPayload);
                payload.put("nonce", System.currentTimeMillis()); // Уникальный номер
                // Подпись (упрощенно, в полной версии нужно подписывать приватным ключом клиента)
                payload.put("signature", ""); 

                URL url = new URL(BASE_URL + "/?act=secure_send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject json = new JSONObject(response.toString());
                    if ("delivered".equals(json.getString("status"))) {
                        Log.d(TAG, "Message delivered successfully");
                        callback.onSuccess(json.toString());
                        
                        // Если сервер потребовал новое рукопожатие
                        if ("request_new_handshake".equals(json.getString("next_action"))) {
                            currentSessionId = null;
                            serverPublicKey = null;
                        }
                    } else {
                        throw new Exception("Send failed: " + json.toString());
                    }
                } else {
                    throw new Exception("HTTP Error: " + code);
                }
            } catch (Exception e) {
                Log.e(TAG, "Send error", e);
                callback.onError(e);
            }
        }).start();
    }
}
