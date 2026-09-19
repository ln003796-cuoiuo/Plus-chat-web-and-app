<?php
/**
 * ПЛЮСЧАТ.РФ - Основной Хостинг (Приемник данных)
 * 
 * НАЗНАЧЕНИЕ:
 * 1. Принимает ТОЛЬКО от доверенного шлюза (прото.плюсчат.рф).
 * 2. Проверяет подпись шлюза.
 * 3. Снимает внешний слой защиты.
 * 4. Сохраняет/обрабатывает сообщение (база данных, файлы).
 * 
 * ВАЖНО: Этот файл НЕ доступен напрямую из интернета для клиентов.
 * Доступ разрешен только по внутреннему токену от шлюза.
 */

header('Content-Type: application/json');

// Секретный токен (должен совпадать с тем, что в шлюзе)
// В продакшене вынести в .env или config.php вне корня сайта
define('STATIC_GATEWAY_TOKEN', 'super_secure_static_token_replace_me_12345');

// Проверка заголовка авторизации от шлюза
$gatewayToken = $_SERVER['HTTP_X_GATEWAY_AUTH'] ?? '';

if ($gatewayToken !== STATIC_GATEWAY_TOKEN) {
    http_response_code(403);
    error_log("Security Alert: Unauthorized access attempt to Host API from IP: " . $_SERVER['REMOTE_ADDR']);
    echo json_encode(['status' => 'error', 'message' => 'Access Denied: Invalid Gateway Token']);
    exit;
}

// Получаем данные от шлюза
$input = file_get_contents('php://input');
$data = json_decode($input, true);

if (!$data || !isset($data['verified']) || $data['verified'] !== true) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'Invalid Gateway Packet']);
    exit;
}

// Проверка подписи шлюза (целостность передачи)
$payload = $data['encrypted_content'];
$gatewaySign = $data['gateway_signature'];

$expectedSign = hash_hmac('sha256', $payload, STATIC_GATEWAY_TOKEN);

if (!hash_equals($expectedSign, $gatewaySign)) {
    http_response_code(403);
    error_log("Security Alert: Gateway signature mismatch. Possible MITM attack.");
    echo json_encode(['status' => 'error', 'message' => 'Data Integrity Check Failed']);
    exit;
}

// --- ДАННЫЕ ПРОВЕРЕНЫ И БЕЗОПАСНЫ ---

// Логика обработки сообщения:
// 1. Расшифровать $payload (если здесь хранится приватный ключ для внутреннего слоя AES)
// 2. Сохранить в БД
// 3. Отправить пуш-уведомление получателю

// Пример ответа (в реальном проекте тут логика сохранения)
$response = [
    'status' => 'stored',
    'message_id' => bin2hex(random_bytes(16)),
    'server_timestamp' => time(),
    'debug_info' => 'Received from Gateway IP: ' . ($_SERVER['HTTP_X_FORWARDED_FOR'] ?? 'Unknown')
];

// Логирование факта приема (без содержания!)
file_put_contents(__DIR__ . '/../logs/host_access.log', date('Y-m-d H:i:s') . " - Message received\n", FILE_APPEND);

echo json_encode($response, JSON_UNESCAPED_UNICODE);
?>
