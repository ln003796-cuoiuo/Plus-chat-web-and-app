<?php
/**
 * ОСНОВНОЙ ХОСТ (pluschat.ru/api/receive.php)
 * Функция: Прием данных от ПРОТО-шлюза, обработка логики мессенджера.
 */

// Проверка, что запрос пришел именно от нашего шлюза (по заголовку)
$gatewayStatus = $_SERVER['HTTP_X_GATEWAY_STATUS'] ?? '';
if ($gatewayStatus !== 'VERIFIED') {
    http_response_code(403);
    die('Direct access denied. Only Gateway allowed.');
}

// Получаем сырые данные (уже расшифрованные шлюзом от внешнего слоя)
$rawData = file_get_contents('php://input');
$sessionId = $_SERVER['HTTP_X_SESSION_ID'] ?? 'unknown';

// Логика обработки (ЗАГОТОВКА)
// Здесь будет логика регистрации, входа, отправки сообщений
// Сейчас просто эмулируем ответ

$response = [
    'status' => 'ok',
    'session' => $sessionId,
    'message' => 'Data received securely via Proto-Gateway',
    'timestamp' => time()
];

echo json_encode($response);
