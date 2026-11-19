# Valorant Tactical Hub — Starter Kit

Навчальний мікросервісний монорепозиторій (`ua.cdu.edu.fotius.fspm.valorant`).

**Технологічний стек:** Java 25, Gradle 9.9.1, Micronaut 5.0.2 (`jakarta.inject`), Jib 3.5.2, PostgreSQL 18, Flyway, JDBI 3, Testcontainers, Vite + TailwindCSS.

## Архітектура та Модулі

1. **`roster-service`** (Port 8080): Контекст *Tactical Draft*. Машина станів (`DRAFT` -> `LOCKED` -> `ARCHIVED`), HATEOAS HAL (`_links`), рольова модель (`CAPTAIN` / `PLAYER`).
2. **`match-analytics-service`** (Port 8081): Контекст *Match History*. Валідація результату матчу, міжсервісний REST-виклик в `rank-calculator-service`, перехід стану `roster-service` в `ARCHIVED` після фіксації гри.
3. **`rank-calculator-service`** (Port 8082): *Stateless Pure Domain Service*. Обчислення дельти рейтингу (RR), перформанс-бонуси, штрафи, просування по рангу (TDD).
4. **`frontend`**: Tactical HUD Console на Vite, Vanilla JS та TailwindCSS. Dynamic UI керується наявністю `_links`. Реалізує **Fault Tolerance & Offline-First Seed** (миттєвий рендер пріоритетного пулу мета-агентів із committed seed, фонова гідрація з Riot Games API та локальне кешування).

---

## 0. Початок роботи з GitHub та елементи GitOps (GitHub Actions CI)

Робота над лабораторними виконується у вашому **власному форку** репозиторію (без створення Pull Request у оригінальний репозиторій викладача):

1. Натисніть кнопку **Fork** у правому верхньому куті цього репозиторію на GitHub та створіть форк у власному акаунті.
2. Склонуйте **свій** форк локально:
   ```bash
   git clone https://github.com/<ВАШ_ЛОГІН>/vth-cnu-2026.git
   cd valorant-tactical-hub
   ```
3. *(Необовʼязково)* Додайте оригінальний репозиторій викладача як `upstream`, щоб за потреби підтягувати оновлення:
   ```bash
   git remote add upstream https://github.com/pnedonosko/vth-cnu-2026.git
   ```
4. *(Необовʼязково)* Увімкніть GitHub Actions CI у своєму форку:
   * Відкрийте вкладку **Actions** у вашому форку на GitHub та натисніть зелену кнопку *«I understand my workflows, go ahead and enable them»*.
   * Тепер при кожному `git push` GitHub автоматично компілюватиме всі три сервіси, запускатиме Unit + Testcontainers тести та перевірятиме збірку фронтенду.
5. **Безпека секретів:** ніколи не комітьте файл `.env.cloud` із паролями від Neon PostgreSQL (він уже доданий у `.gitignore`).

---

## Архітектурна структура та навчальні завдання

1. **`roster-service` (Зразок Pragmatic DDD + Light Hexagonal):**
   * `domain/` — Aggregate Root `Roster`, Value Objects (`AgentPick`, `RosterId`, `PlayerId`, `AgentKey`), порт до БД `RosterRepository`.
   * `application/` — `RosterApplicationService` (оркестрація Use Cases через `jakarta.inject`).
   * `infrastructure/` — `JdbiRosterRepository` (JDBI 3 + HikariCP) та `RosterController` (REST, HAL `_links`, Micronaut Validation).
2. **`match-analytics-service` (Лабораторна робота №1 — Рефакторинг у DDD):**
   * Навмисно реалізований у стилі *Transaction Script* (SQL через JDBI 3 прямо в `MatchController`).
   * **Завдання:** винести бізнес-логіку та інваріанти в доменну модель, виділити порт репозиторію та `MatchApplicationService` за зразком `roster-service`.
3. **`rank-calculator-service` (Лабораторна робота №2 — Перехід на DI):**
   * Використовує `@FunctionalInterface RatingPolicy` зі статичним методом `defaultCompetitivePolicy`.
   * **Завдання:** перевести розрахунок рейтингу на інжектовані `@Singleton` біни через `jakarta.inject` з конфігурованими коефіцієнтами.

---

## 1. Ініціалізація та локальний запуск (Після Лекції "Плануємо мікросервісний додаток")

1. **Ініціалізуйте залежності проєкту** (один раз після `git clone`):
   ```bash
   make init
   ```
2. **Запустіть ізольовані бази даних PostgreSQL:**
   ```bash
   make up-db
   ```
3. **Запустіть мікросервіси** (у трьох окремих терміналах):
   ```bash
   make run-roster      # http://localhost:8080
   make run-analytics   # http://localhost:8081
   make run-rank        # http://localhost:8082
   ```
4. **Запустіть Frontend SPA та Swagger UI:**
   ```bash
   make dev-frontend    # http://localhost:3000 (повний стек із бекендом)
   make dev-mock        # http://localhost:3000 (автономний режим без бекенду)
   make dev-api         # http://localhost:3001 (Swagger UI OpenAPI 3.1)
   ```
5. **Запуск тестів** (Unit + Testcontainers PostgreSQL 18):
   ```bash
   make test
   ```

---

## 2. Налаштування Хмари: Google Cloud Run + Neon PostgreSQL

Кожен студент розгортає власну копію системи у своєму Google Cloud проєкті та безкоштовному інстансі **Neon**.

### Крок А. Створення баз даних у Neon
1. Зареєструйтесь на [neon.tech](https://neon.tech) та створіть новий проєкт (**PostgreSQL 18**, регіон бажано обрати найближчий до вашого Cloud Run, наприклад `aws-us-east-1` для `us-central1`).
2. У вкладці **Databases** створіть дві окремі бази даних (дотримання *Database per Service*):
   * `roster_db`
   * `analytics_db`
3. На вкладці **Dashboard / Connection Details** скопіюйте **Direct connection** рядок (важливо: **вимкніть** галочку *Connection pooling* / приберіть `-pooler` з хоста, оскільки Flyway використовує `pg_advisory_lock`).

### Крок Б. Підготовка Google Cloud (`gcloud`)
1. Створіть проєкт у [Google Cloud Console](https://console.cloud.google.com) та авторизуйтесь у CLI:
   ```bash
   gcloud auth login
   gcloud config set project YOUR_PROJECT_ID
   ```
2. Увімкніть необхідні API (Cloud Run та Artifact Registry):
   ```bash
   gcloud services enable run.googleapis.com artifactregistry.googleapis.com
   ```
3. Створіть репозиторій для Docker-образів (один раз на проєкт):
   ```bash
   gcloud artifacts repositories create valorant-repo \
     --repository-format=docker \
     --location=us-central1
   gcloud auth configure-docker us-central1-docker.pkg.dev
   ```

### Крок В. Конфігурація та автоматичний деплой
1. Скопіюйте шаблон змінних оточення:
   ```bash
   cp .env.cloud.example .env.cloud
   ```
2. Впишіть у `.env.cloud` ваш `GCP_PROJECT_ID`, `NEON_ROSTER_JDBC_URL`, `NEON_ANALYTICS_JDBC_URL`, `NEON_DB_USER` та `NEON_DB_PASSWORD`.
3. Зберіть Docker-образи через Jib (`make build-docker`) або одразу виконайте повний деплой у Cloud Run однією командою:
   ```bash
   make deploy-cloud
   ```
   *Під час старту в хмарі сервіси активують профіль `VTH_ENVIRONMENT=cloud` (`application-cloud.yml`), де налаштований пул HikariCP під serverless cold-start Neon, а Flyway автоматично застосує SQL-міграції.*
4. Отримані в консолі URL-адреси впишіть у `frontend/.env.cloud` та задеплойте фронтенд у Firebase Hosting (`make deploy-frontend`).
