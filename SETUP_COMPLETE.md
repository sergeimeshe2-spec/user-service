# User Service - Setup Complete

## Готово! Все работает

### GitHub репозиторий
- **URL**: https://github.com/sergeimeshe2-spec/user-service.git
- **Содержит**:
  - Полный код сервиса (Spring Boot)
  - OpenAPI спецификация (`src/main/resources/specs/user-service-openapi.yaml`)
  - AsyncAPI спецификация (`src/main/resources/specs/user-events-asyncapi.yaml`)

### Запущенные сервисы
```
✅ user-service-app          - REST API на порту 8081
✅ user-service-microcks     - Microcks UI на порту 8080
✅ user-service-async-minion - Async Minion на порту 8081 (внутри сети)
✅ user-service-kafka        - Kafka на порту 9092
✅ user-service-mongo        - MongoDB
✅ user-service-microcks-sync - Cron scheduler (каждые 10 мин)
```

## Как это работает

### Каждые 10 минут:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Microcks Sync Scheduler (cron: 0 */10 * * *)            │
│    - Клонирует/обновляет GitHub репозиторий                 │
│    - Вычисляет hash спецификаций                            │
│    - Сравнивает с предыдущим hash                           │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ├── Hash изменился? ──► НЕТ ──► Пропустить
                  │
                  └─── ДА ──► Импортировать в Microcks:
                              - OpenAPI spec → REST mocks
                              - AsyncAPI spec → Kafka mocks
```

### URLs

| Сервис | URL | Описание |
|--------|-----|----------|
| User Service | http://localhost:8081 | Реальный сервис |
| Microcks UI | http://localhost:8080 | UI Microcks |
| REST API Mocks | http://localhost:8080/rest/User+Service/1.0.0 | Моки REST API |
| Async Minion | http://localhost:8081 (внутри сети) | Моки Async API |
| Kafka | localhost:9092 | Kafka broker |

## Изменение спецификаций

1. **Измените спецификацию** в проекте:
   ```bash
   cd C:\project\user-service
   # Отредактируйте файл:
   # src/main/resources/specs/user-service-openapi.yaml
   # или
   # src/main/resources/specs/user-events-asyncapi.yaml
   ```

2. **Запушьте в GitHub**:
   ```bash
   git add src/main/resources/specs/
   git commit -m "Update API spec"
   git push origin main
   ```

3. **Подождите до 10 минут** - Microcks автоматически обновит моки

   Или **запустите синхронизацию вручную**:
   ```bash
   docker exec user-service-microcks-sync /sync-microcks.sh
   ```

## Тестирование

### Тест REST API мока
```bash
curl http://localhost:8080/rest/User+Service/1.0.0/users
```

### Тест реального сервиса
```bash
curl http://localhost:8081/api/v1/users
```

### Проверка логов синхронизации
```bash
docker logs user-service-microcks-sync -f
# Или
docker exec user-service-microcks-sync cat /var/log/sync.log
```

## Структура проекта

```
user-service/
├── src/main/
│   ├── java/com/example/userservice/
│   │   ├── UserServiceApplication.java
│   │   ├── controller/UserController.java
│   │   ├── model/User.java
│   │   └── service/
│   │       ├── UserService.java
│   │       └── UserEventProducer.java
│   └── resources/
│       ├── application.properties
│       └── specs/                    ← Спецификации для Microcks
│           ├── user-service-openapi.yaml
│           └── user-events-asyncapi.yaml
├── scripts/
│   ├── sync-microcks.sh              ← Linux/Mac cron script
│   └── sync-microcks.ps1             ← Windows script
├── config/
│   └── application.properties        ← Async Minion config
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Логика автоматического обновления

### Скрипт `sync-microcks.sh`:

```bash
# 1. Проверить GitHub на изменения
git fetch origin
git reset --hard origin/main

# 2. Вычислить hash всех спецификаций
CURRENT_HASH=$(find specs/ -name "*.yaml" -exec cat {} \; | md5sum)

# 3. Сравнить с сохраненным hash
if [ "$CURRENT_HASH" = "$LAST_HASH" ]; then
    exit 0  # Нет изменений
fi

# 4. Если есть изменения:
#    - Импортировать OpenAPI spec
#    - Импортировать AsyncAPI spec
#    - Сохранить новый hash
```

### Cron выражение:
```
0 */10 * * * *  # Каждые 10 минут
```

## Полезные команды

```bash
# Статус всех сервисов
cd C:\project\user-service && docker-compose ps

# Логи конкретного сервиса
docker logs user-service-microcks-sync -f

# Ручная синхронизация
docker exec user-service-microcks-sync /sync-microcks.sh

# Перезапуск
docker-compose restart

# Остановка
docker-compose down
```

## Пример изменения спецификации

```bash
# 1. Изменить спецификацию
notepad C:\project\user-service\src\main\resources\specs\user-service-openapi.yaml

# 2. Запушить в GitHub
cd C:\project\user-service
git add src/main/resources/specs/
git commit -m "Add new endpoint to API spec"
git push origin main

# 3. Дождаться автоматической синхронизации (до 10 мин)
# или запустить вручную:
docker exec user-service-microcks-sync /sync-microcks.sh

# 4. Проверить обновление
curl http://localhost:8080/rest/User+Service/1.0.0/users
```

---

**Все готово!** Сервис автоматически обновляет моки каждые 10 минут при изменении спецификаций в GitHub.
