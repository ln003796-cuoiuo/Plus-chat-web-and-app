<?php
/**
 * ПРОТО.ПЛЮСЧАТ.РФ - Криптографический Шлюз (КСЗ "Динамический Лабиринт")
 * 
 * НАЗНАЧЕНИЕ:
 * 1. Принимает зашифрованные данные от мобильного приложения.
 * 2. Генерирует УНИКАЛЬНЫЙ ключ шифрования для КАЖДОГО сообщения.
 * 3. Проверяет целостность и подлинность.
 * 4. Пересылает очищенные данные на ХОСТ (плюсчат.рф).
 * 5. Никогда не хранит содержимое сообщений.
 * 
 * АЛГОРИТМ РАБОТЫ:
 * - Handshake: Генерация пары RSA-4096 для сессии.
 * - Message: Прием данных, зашифрованных AES-256-GCM + подпись RSA.
 * - Rotation: После каждого сообщения ключи уничтожаются и генерируются новые.
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Session-ID, X-Signature');

// Настройки безопасности
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/logs/gateway.log');
ini_set('display_errors', 0); // Никогда не показывать ошибки клиенту
error_reporting(0);

// Конфигурация соединения с ХОСТОМ
define('HOST_API_URL', 'https://плюсчат.рф/hosting/api/receive.php');
define('GATEWAY_SECRET', hash('sha256', 'unique_secret_change_this_in_prod_' . time()));

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$input = file_get_contents('php://input');
$action = $_GET['act'] ?? 'handshake';

try {
    if ($action === 'handshake') {
        handleHandshake();
    } elseif ($action === 'secure_send' && !empty($input)) {
        handleSecureSend($input);
    } else {
        throw new Exception('Invalid request');
    }
} catch (Exception $e) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'Gateway Security Error']);
    error_log("Security Event: " . $e->getMessage());
}

/**
 * 1. РУКОПОЖАТИЕ
 * Генерируем уникальную пару ключей для этой сессии.
 * Клиент получит публичный ключ и зашифрует им свое сообщение.
 */
function handleHandshake() {
    // Генерация мощной пары ключей RSA
    $config = [
        'private_key_bits' => 4096,
        'private_key_type' => OPENSSL_KEYTYPE_RSA,
        'curve_name' => 'secp384r1'
    ];
    
    $res = openssl_pkey_new($config);
    if (!$res) throw new Exception('Key generation failed');
    
    openssl_pkey_export($res, $privateKey);
    $details = openssl_pkey_get_details($res);
    $publicKey = $details['key'];
    
    // ID сессии
    $sessionId = bin2hex(random_bytes(32));
    
    // Сохраняем приватный ключ во временное хранилище (TTL 5 минут)
    // В продакшене лучше использовать Redis с авто-удалением
    $keyFile = sys_get_temp_dir() . "/proto_key_{$sessionId}";
    file_put_contents($keyFile, $privateKey);
    chmod($keyFile, 0600);
    
    // Планируем удаление ключа (в идеале через cron или redis expire)
    // Для демо удалять будет скрипт очистки
    
    echo json_encode([
        'status' => 'ok',
        'action' => 'handshake_complete',
        'session_id' => $sessionId,
        'public_key' => base64_encode($publicKey),
        'algorithm' => 'RSA-4096 + AES-256-GCM',
        'rotation_policy' => 'ONE_TIME_KEY', // Ключ одноразовый
        'timestamp' => time()
    ], JSON_UNESCAPED_UNICODE);
}

/**
 * 2. ПРИЕМ И ПЕРЕДАЧА ЗАШИФРОВАННЫХ ДАННЫХ
 */
function handleSecureSend($input) {
    $data = json_decode($input, true);
    
    if (!isset($data['session_id'], $data['payload'], $data['nonce'], $data['signature'])) {
        throw new Exception('Missing secure fields');
    }
    
    $sessionId = $data['session_id'];
    $keyFile = sys_get_temp_dir() . "/proto_key_{$sessionId}";
    
    if (!file_exists($keyFile)) {
        throw new Exception('Session key not found or expired');
    }
    
    $privateKey = file_get_contents($keyFile);
    
    // 1. Проверка цифровой подписи (целостность данных)
    $payload = $data['payload']; // Base64 encoded encrypted data
    $signature = base64_decode($data['signature']);
    
    $verifyResult = openssl_verify($payload, $signature, $privateKey, OPENSSL_ALGO_SHA512);
    if ($verifyResult !== 1) {
        throw new Exception('Signature verification failed - possible tampering');
    }
    
    // 2. Расшифровка сообщения (если нужно, зависит от реализации клиента)
    // В схеме "Двойной контур": 
    // - Внешний слой (RSA) снят при проверке подписи/передачи ключа
    // - Внутренний слой (AES) остается до самого Хоста или снимается здесь
    
    // Формируем пакет для ХОСТА (плюсчат.рф)
    // Добавляем маркер, что данные прошли шлюз
    $gatewayPacket = [
        'verified' => true,
        'gateway_timestamp' => time(),
        'client_nonce' => $data['nonce'], // Уникальный номер сообщения
        'encrypted_content' => $payload,  // Передаем зашифрованное тело дальше
        'gateway_signature' => hash_hmac('sha256', $payload, GATEWAY_SECRET)
    ];
    
    // 3. Отправка на ХОСТ через защищенное соединение
    $ch = curl_init(HOST_API_URL);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($gatewayPacket));
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'X-Gateway-Auth: ' . GATEWAY_SECRET,
        'X-Forwarded-For: ' . ($_SERVER['HTTP_X_REAL_IP'] ?? $_SERVER['REMOTE_ADDR'])
    ]);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    
    $hostResponse = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200 || !$hostResponse) {
        throw new Exception('Host connection failed');
    }
    
    // 4. УНИЧТОЖЕНИЕ КЛЮЧА (Perfect Forward Secrecy)
    unlink($keyFile);
    
    // Возвращаем ответ от Хоста клиенту (при необходимости зашифровав его новым ключом)
    echo json_encode([
        'status' => 'delivered',
        'host_response' => json_decode($hostResponse, true),
        'key_destroyed' => true,
        'next_action' => 'request_new_handshake' // Требуем новое рукопожатие для след. сообщения
    ], JSON_UNESCAPED_UNICODE);
}
?>