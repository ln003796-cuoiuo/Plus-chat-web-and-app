# PlusChat Secure Messenger (Android)

## Архитектура безопасности "Динамический Лабиринт"

### Компоненты:
1. **App (APK)** - Модифицированный клиент Telegram Android с заменой сетевого стека.
2. **Прото.Плюсчат.РФ** (`xn--n1aabel.xn--80avljg2a1c.xn--p1ai`) - Криптографический шлюз.
3. **Плюсчат.РФ** (`xn--80avljg2a1c.xn--p1ai`) - Основной хостинг (хранение данных).

### Схема работы:
```
[Приложение] 
   │
   ├─(1. Handshake)─> [Прото.Плюсчат.РФ] <─(TLS)─> [Генерация RSA-4096]
   │                        │
   ├─(2. Зашифрованное сообщение)─> │
   │                        ├─ Проверка подписи
   │                        ├─ Уничтожение ключа сессии
   │                        │
   │                        └─(3. Передача)─> [Плюсчат.РФ/API]
   │                                           ├─ Проверка токена шлюза
   │                                           └─ Сохранение в БД
```

## Интеграция в проект Telegram Android

### 1. Добавить файлы безопасности
Скопируйте папку `com/pluschat/security` в:
`TMessagesProj/src/main/java/com/pluschat/security/`

Файлы:
- `CryptoManager.java` - Шифрование AES-GCM + RSA.
- `SecureApiClient.java` - HTTP-клиент для связи со шлюзом.

### 2. Заменить сетевой вызов в MessagesController
Найдите метод отправки сообщения (обычно `sendMessage` или `sendRpcRequest`) и замените прямой вызов MTProto на:

```java
import com.pluschat.security.SecureApiClient;
import com.pluschat.security.CryptoManager;

// ... внутри метода отправки ...

SecureApiClient client = new SecureApiClient();

client.handshake(new SecureApiClient.Callback<SecureApiClient.HandshakeResult>() {
    @Override
    public void onSuccess(SecureApiClient.HandshakeResult result) {
        // Рукопожатие успешно, шифруем и отправляем
        client.sendSecureMessage(messageText, new SecureApiClient.Callback<String>() {
            @Override
            public void onSuccess(String response) {
                // Сообщение доставлено на сервер
                Log.d("PlusChat", "Sent: " + response);
            }

            @Override
            public void onError(Exception e) {
                Log.e("PlusChat", "Error", e);
            }
        });
    }

    @Override
    public void onError(Exception e) {
        Log.e("PlusChat", "Handshake failed", e);
    }
});
```

### 3. Настройка GitHub Actions для сборки APK
Создайте файл `.github/workflows/build.yml` в корне репозитория:

```yaml
name: Build PlusChat APK

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'adopt'
          
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
        
      - name: Build with Gradle
        run: ./gradlew assembleRelease
        
      - name: Upload APK
        uses: actions/upload-artifact@v2
        with:
          name: pluschat-app
          path: TMessagesProj/build/outputs/apk/release/*.apk
```

## Развертывание на хостинге

### 1. Прото.Плюсчат.РФ
- Загрузить содержимое `/workspace/host/proto_pluschat_ru/` в корень поддомена.
- Убедиться, что PHP поддерживает OpenSSL.
- Папка `/logs` должна быть доступна для записи (chmod 755).
- Настроить HTTPS (Let's Encrypt).

### 2. Плюсчат.РФ
- Загрузить содержимое `/workspace/host/pluschat_ru/` в подпапку `/hosting/`.
- Файл `api/receive.php` должен быть доступен только внутренним запросам (настроить `.htaccess` или firewall).
- Изменить `STATIC_GATEWAY_TOKEN` в обоих файлах на одинаковый сложный ключ.

## Важные замечания по безопасности
1. **Ключи**: В продакшене замените временные ключи на статические, хранящиеся вне кода (`.env`).
2. **HTTPS**: Обязательно используйте SSL-сертификаты на обоих доменах.
3. **Очистка**: Настройте Cron-скрипт для очистки `/tmp/proto_key_*` каждые 5 минут.
4. **Бэкенд**: Данный код реализует транспортную безопасность. Для полноценного мессенджера нужна реализация хранения сообщений, регистрации пользователей и базы данных на стороне `плюсчат.рф`.
