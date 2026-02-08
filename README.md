# User Service with Microcks Auto-Sync

User Service с автоматическим обновлением моков в Microcks каждые 10 минут при изменении спецификаций в GitHub.

## Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                         GitHub                                  │
│           sergeimeshe2-spec/user-service.git                    │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ src/main/resources/specs/                              │    │
│  │  - user-service-openapi.yaml                           │    │
│  │  - user-events-asyncapi.yaml                           │    │
│  └────────────────────────────────────────────────────────┘    │
└────────────────────────┬────────────────────────────────────────┘
                         │ Каждые 10 минут
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Microcks Sync Scheduler                            │
│         (Проверяет изменения в GitHub)                         │
└────────────────────────┬────────────────────────────────────────┘
                         │ Если есть изменения
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Microcks                                  │
│     http://localhost:8080                                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  REST API Mocks: /rest/User+Service/1.0.0              │    │
│  │  Async API Mocks: Kafka topics                         │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Компоненты

### 1. User Service
- **Порт**: 8081
- **Технологии**: Spring Boot 3.2, Kafka
- **REST API**:
  - `GET /api/v1/users` - Получить всех пользователей
  - `POST /api/v1/users` - Создать пользователя
  - `GET /api/v1/users/{userId}` - Получить пользователя по ID
  - `PUT /api/v1/users/{userId}` - Обновить пользователя
  - `DELETE /api/v1/users/{userId}` - Удалить пользователя
  - `PATCH /api/v1/users/{userId}/profile` - Обновить профиль

- **Async Events** (Kafka topics):
  - `user/created` - Событие создания пользователя
  - `user/updated` - Событие обновления пользователя
  - `user/deleted` - Событие удаления пользователя
  - `user/profile-updated` - Событие обновления профиля

### 2. Microcks
- **Порт**: 8080
- **Функция**: Автоматическое создание и обновление моков
- **Автообновление**: Каждые 10 минут

### 3. Sync Scheduler
- **Функция**: Проверка изменений в GitHub каждые 10 минут
- **Действие**: Обновление моков в Microcks при изменении спецификаций

## Быстрый старт

### 1. Клонирование и запуск

```bash
# Клонировать репозиторий
git clone https://github.com/sergeimeshe2-spec/user-service.git
cd user-service

# Запустить все сервисы
docker-compose up -d
```

### 2. Проверка статуса

```bash
# Проверить что все сервисы работают
docker-compose ps

# Должны быть healthy:
# - user-service-kafka
# - user-service-mongo
# - user-service-microcks
# - user-service-microcks-postman
# - user-service-async-minion
# - user-service-app
# - user-service-microcks-sync
```

### 3. Использование

#### REST API Mocks
```bash
# Получить всех пользователей
curl http://localhost:8080/rest/User+Service/1.0.0/users

# Создать пользователя
curl -X POST http://localhost:8080/rest/User+Service/1.0.0/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ivan","email":"ivan@example.com"}'
```

#### Async API Mocks
```bash
# Проверить Async Minion
curl http://localhost:8081/q/health/ready

# Отправить событие в Kafka
kafka-console-producer --broker-list localhost:9092 --topic user/created
```

## Логика автоматического обновления

### Каждые 10 минут:

1. **Sync Scheduler** проверяет GitHub репозиторий
2. **Вычисляет hash** всех файлов спецификаций
3. **Сравнивает** с предыдущим hash

### Если есть изменения:

1. **Клонирует/обновляет** локальную копию репозитория
2. **Импортирует** спецификации в Microcks:
   - `user-service-openapi.yaml` → REST API mocks
   - `user-events-asyncapi.yaml` → Async API mocks
3. **Сохраняет** новый hash

### Если нет изменений:

- Пропускает обновление (ничего не делает)

## Ручное обновление моков

```bash
# Запустить sync скрипт вручную
./scripts/sync-microcks.sh

# Или PowerShell версия
powershell -ExecutionPolicy Bypass -File scripts/sync-microcks.ps1
```

## Изменение спецификаций

1. Измените файлы в `src/main/resources/specs/`:
   - `user-service-openapi.yaml`
   - `user-events-asyncapi.yaml`

2. Запушьте изменения в GitHub:
   ```bash
   git add src/main/resources/specs/
   git commit -m "Update API specifications"
   git push origin main
   ```

3. **Подождите до 10 минут** - Microcks автоматически обновит моки

   Или запустите синхронизацию вручную.

## Структура проекта

```
user-service/
├── src/main/
│   ├── java/com/example/userservice/
│   │   ├── UserServiceApplication.java    # Главный класс
│   │   ├── controller/
│   │   │   └── UserController.java         # REST контроллер
│   │   ├── model/
│   │   │   └── User.java                   # Модель пользователя
│   │   └── service/
│   │       ├── UserService.java            # Бизнес-логика
│   │       └── UserEventProducer.java      # Kafka producer
│   └── resources/
│       ├── application.properties          # Конфигурация
│       └── specs/
│           ├── user-service-openapi.yaml   # REST API спецификация
│           └── user-events-asyncapi.yaml   # Async API спецификация
├── scripts/
│   ├── sync-microcks.sh                    # Linux/Mac sync скрипт
│   └── sync-microcks.ps1                   # Windows sync скрипт
├── Dockerfile                              # Контейнеризация
├── docker-compose.yml                      # Все сервисы
└── pom.xml                                 # Maven конфигурация
```

## Мониторинг

### Логи синхронизации
```bash
# Логи sync scheduler
docker logs user-service-microcks-sync -f

# Или查看 cron логи
docker exec user-service-microcks-sync cat /var/log/sync.log
```

### Логи Microcks
```bash
docker logs user-service-microcks -f
```

## Устранение проблем

### Микрокс не обновляется
```bash
# Проверить логи sync scheduler
docker logs user-service-microcks-sync

# Запустить sync вручную
docker exec user-service-microcks-sync /sync-microcks.sh
```

### Проверить статус сервисов
```bash
docker-compose ps
docker-compose logs
```

### Перезапуск сервисов
```bash
docker-compose restart
```

## Полезные команды

```bash
# Остановить все сервисы
docker-compose down

# Остановить и удалить volumes
docker-compose down -v

# Пересобрать и запустить
docker-compose up -d --build

# Просмотреть логи конкретного сервиса
docker-compose logs -f user-service
docker-compose logs -f microcks
```

## URLs после запуска

- **User Service**: http://localhost:8081
- **Microcks UI**: http://localhost:8080
- **Microcks API**: http://localhost:8080/api
- **Async Minion**: http://localhost:8081
- **Kafka**: localhost:9092

## Лицензия

MIT License
