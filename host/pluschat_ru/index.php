<?php
// pluschat_ru/index.php
// Главный сайт плюсчат.рф

header('Content-Type: text/html; charset=utf-8');
?>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ПлюсЧат - Твой безопасный мессенджер</title>
    <style>
        body { font-family: sans-serif; text-align: center; padding: 50px; background: #f0f2f5; }
        .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: #3390ec; }
        .btn { display: inline-block; padding: 15px 30px; background: #3390ec; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
        .btn:hover { background: #2775c9; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Добро пожаловать в ПлюсЧат</h1>
        <p>Современный мессенджер с открытым исходным кодом.</p>
        <p>Скачайте наше приложение или используйте веб-версию.</p>
        
        <a href="/webapp/" class="btn">Открыть Веб-версию</a>
        <br><br>
        <small>Сервер защищен HTTPS. IP: 195.3.246.75</small>
    </div>
</body>
</html>
