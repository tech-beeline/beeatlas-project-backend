# project-backend

REST-сервис для управления проектами и оценками.

## Назначение

Сервис обеспечивает:

- **Управление проектами** — создание, фильтрация, добавление участников, генерация уникальных идентификаторов формата `PROJ.XX.XX.XX`
- **Оценка бизнес-постановок** — структурирование функциональных/нефункциональных требований, технических возможностей (TC), бизнес-возможностей (BC), открытых вопросов (OQ)
- **Управление Use Case** — создание/обновление сценариев в ветках проектов с публикацией обнаруженных интерфейсов в смежный сервис `fdm-products`
- **Ветвление артефактов** — поддержка веток `main`, `design`, `develop`, `user`

## Технологический стек

| Категория | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.4.3 |
| Сборка | Maven |
| БД | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Миграции | Flyway |
| API-документация | SpringDoc OpenAPI (Swagger UI) |
| Кодогенерация | Lombok |
| Мониторинг | Actuator + Micrometer + Prometheus |
| Трассировка | OpenTelemetry |
| CI/CD | GitLab CI/CD |
| Анализ кода | SonarQube |

## Архитектура

```
ru.beeline.projectbackend/
├── controller/       # REST-контроллеры
├── service/          # Бизнес-логика
├── repository/       # Spring Data JPA репозитории
├── domain/           # JPA-сущности
├── dto/              # Объекты передачи данных
├── client/           # HTTP-клиенты к внешним сервисам
├── config/           # Конфигурация приложения
├── exception/        # Пользовательские исключения
└── utils/            # Утилиты
```

## API Endpoints

### Проект (`/api/v1/project`)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/v1/project` | Создание проекта. Заголовок `user-id` — владелец. Генерируется `uniqueIdent` формата `PROJ.XX.XX.XX`. Автоматически создаётся ветка `main`. |
| `POST` | `/api/v1/project/{projectId}/user/{userId}` | Добавление пользователя к проекту |
| `GET` | `/api/v1/project` | Список проектов с фильтрацией по `status-id`, `owner-id` |
| `GET` | `/api/v1/project/{id}` | Детальная информация о проекте |

### Оценка (`/api/v1/assessments`)

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/v1/assessments` | Список оценок с фильтрацией по `project-id`, `status-id`, `owner-id`. Сводные счётчики FR, NFR, TC, продуктов, OQ. |
| `GET` | `/api/v1/assessments/{id}` | Детальная информация: требования, TC, design TC, открытые вопросы |
| `POST` | `/api/v1/assessments` | Создание оценки. Заголовок `user-id`. Валидация `source` (text/confluence), `impactLevel` (S/M/L/XL). Статус: `Done`. |

### Use Case (`/api/v1/project`)

| Метод | Путь | Описание |
|---|---|---|
| `PUT` | `/api/v1/project/{projectId}/use-case` | Создание/обновление use case в ветке (`branch`, по умолчанию `main`). Публикация операций в `fdm-products` внутри транзакции. |

### Внутренние

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/v1/internal/check/project/{id}/owner?userId=` | Проверка принадлежности проекта пользователю |

## Модель данных

Все таблицы в схеме `projects`.

| Сущность | Таблица | Описание |
|---|---|---|
| `Project` | `project` | Проект: название, описание, владелец, статус, уникальный идентификатор |
| `ProjectUser` | `project_user` | Связь проект-пользователь (многие-ко-многим) |
| `ProjectStatusEnum` | `project_status_enum` | Справочник статусов проекта: `Backlog`, `InWork`, `Done` |
| `Assessment` | `assessments` | Оценка: источник, текст, уровень влияния, статус |
| `AssessmentStatusEnum` | `assessment_status_enum` | Справочник статусов оценки: `Draft`, `REQ`, `TC`, `Done` |
| `RequirementFunc` | `requirements_func` | Функциональные требования |
| `RequirementNonFunc` | `requirements_non_func` | Нефункциональные требования |
| `OpenQuestion` | `open_questions` | Открытые вопросы |
| `AssessmentTc` | `assessment_tc` | Существующие технические возможности |
| `AssessmentTcDesign` | `assessment_tc_design` | Новые TC, создаваемые пользователем |
| `ArtifactBranch` | `artifact_branch` | Ветвление артефактов |
| `UseCase` | `use_case` | Сценарии использования |
| `UsOperationRelation` | `us_operation_relation` | Шаги use case и связи с операциями |

## Внешние сервисы

| Клиент | Сервис | URL (local) | Назначение |
|---|---|---|---|
| `UserClient` | fdm-auth | `http://fdm-auth-backend:8080` | Получение профилей пользователей |
| `ProductClient` | fdm-products | `http://products-service:8080` | Публикация обнаруженных интерфейсов |

## Запуск

### Требования

- JDK 21
- Maven 3.x
- PostgreSQL

### Локальный запуск

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```


### Конфигурация

Файлы конфигурации:

- `application.properties` — общие настройки


### Сборка

```bash
mvn clean package -DskipTests
```

### Тесты

```bash
mvn test
```


## Swagger UI

После запуска приложения документация API доступна по адресу:

```
http://localhost:8080/swagger-ui.html
```

## Мониторинг

- **Health**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`


