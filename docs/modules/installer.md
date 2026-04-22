# installer

Мульти-лаунчер установщик Vida. Swing-GUI + CLI; поддерживает Mojang Launcher, Prism, MultiMC, ATLauncher, Modrinth App и CurseForge App.

- Пакет: `dev.vida.installer`
- Gradle: `dev.vida:vida-installer`
- Стабильность: `@ApiStatus.Preview`

Руководство для игрока — [getting-started/installation.md](../getting-started/installation.md). Список CLI-флагов — [reference/cli-installer.md](../reference/cli-installer.md). Пользовательская логика по лаунчерам — [guides/multi-launcher.md](../guides/multi-launcher.md).

Эта страница — про модуль.

## Архитектура

```
InstallerMain ─┬─ GUI: Swing InstallerFrame
               └─ CLI: CliArgs → CliInstaller
                          │
                          ▼
                    InstallerCore
                          │
                          ▼
                   LauncherRegistry
                          │
     ┌──────────┬─────────┼─────────┬──────────┬──────────┐
     ▼          ▼         ▼         ▼          ▼          ▼
  Mojang     Prism    MultiMC  ATLauncher  Modrinth  CurseForge
          (extends PrismLikeHandler)
```

`InstallerCore` не знает деталей ни одного лаунчера. Вся логика — в имплементациях `LauncherHandler`.

## Ключевые типы

### `LauncherKind`

Enum поддерживаемых лаунчеров:

```java
enum LauncherKind {
    MOJANG(true), PRISM(true), MULTIMC(true), ATLAUNCHER(true),
    MODRINTH(true), CURSEFORGE(true);
}
```

Все шесть лаунчеров полностью реализованы начиная с 0.4.0.

### `InstallMode`

```java
enum InstallMode {
    CREATE_NEW_PROFILE,
    PATCH_EXISTING_INSTANCE
}
```

Разные лаунчеры поддерживают разные режимы:

- Mojang / Prism / MultiMC — `CREATE_NEW_PROFILE` (создаём профиль/instance с нуля).
- ATLauncher — только `PATCH_EXISTING_INSTANCE` (инсталлятор не создаёт новые инстансы, потому что модпаки там имеют свою структуру).

### `InstanceRef`

«Найденный инстанс в какую-то папке». Запись:

```java
record InstanceRef(
    String id,           // уникальный ключ
    String displayName,  // имя, которое видит пользователь
    Path path,           // путь к инстансу
    String minecraftVersion,
    String loaderInfo    // уже установленный loader, если есть
) { ... }
```

Используется для списка `--list-instances` и для GUI-выбора.

### `LauncherHandler`

Интерфейс — контракт одного лаунчера:

```java
interface LauncherHandler {
    LauncherKind kind();
    Set<InstallMode> supportedModes();
    InstallMode defaultMode();

    List<Path> detectDataDirs();
    List<InstanceRef> listInstances(Path dataDir);

    InstallResult install(InstallOptions opt, ProgressSink progress);
}
```

### `InstallerCore`

Фасад, через который CLI и GUI работают с хэндлерами:

```java
InstallResult r = InstallerCore.install(opt);
```

Выбирает хэндлер из `LauncherRegistry`, валидирует опции, делегирует.

### `InstallerSupport`

Static-утилиты, общие для хэндлеров:

- `extractEmbeddedLoader(Path target)` — достаёт shaded `vida-loader-agent.jar` из ресурсов инсталлятора.
- `writeAtomically(Path target, byte[] content)` — temp-file + rename (для JSON/CFG-файлов лаунчеров).
- `isWindows()` / `isMacOS()` / `isLinux()` — для путей по умолчанию.

### `OsPaths`

Резолвер стандартных директорий лаунчеров на всех ОС. Примеры:

- `OsPaths.prismCandidates()` → `[%APPDATA%/PrismLauncher, ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher, ...]`
- `OsPaths.atLauncherCandidates()` → `[%USERPROFILE%/ATLauncher, ~/Library/ATLauncher, ...]`

## Имплементации

### `MojangHandler`

- Находит `launcher_profiles.json` в `%APPDATA%/.minecraft`.
- Создаёт новую version-директорию `versions/vida-1.21.1/` с JSON-профилем.
- Добавляет/обновляет запись в `launcher_profiles.json`.
- Atomic-write, dry-run поддержка.

### `PrismLikeHandler` (abstract base)

Общая база для Prism и MultiMC. Структура инстанса:

```
<data>/instances/<name>/
    instance.cfg         ← INI-конфиг Prism/MultiMC
    mmc-pack.json        ← список компонентов (Minecraft + Vida)
    patches/
        dev.vida.loader.json  ← патч-компонент Vida
    libraries/
        vida-loader-<ver>.jar
    .minecraft/          ← рабочая директория MC
```

Различия:

- **Prism** ≥ 8.x поддерживает `+agents` в компонент-патче — это чисто, без ручного `JvmArgs`.
- **MultiMC** не поддерживает `+agents`; приходится инжектить `-javaagent` в `JvmArgs` через `instance.cfg`.

Реализация в виде абстрактного `supportsAgents()` — наследники переопределяют.

### `ATLauncherHandler`

Принципиально другой подход: ATLauncher хранит инстансы в `instance.json`, а не в Prism-структуре. Наш плагин:

1. Читает `instance.json`.
2. Находит `launcher.javaArguments`.
3. Добавляет `-javaagent:<path>` (заменяя существующий Vida-агент, если был).
4. Сохраняет через atomic rename.

Особенность: ATLauncher парсит `javaArguments` по пробелам без quoting'а → пути с пробелами отклоняются на этапе валидации.

## Соответствие релизам платформы (0.5.0 — 0.9.0)

Инсталлятор поставляет `-javaagent` и артефакты загрузчика; сценарий установки не менялся между этими минорами, но **появились новые возможности платформы**, с которыми стоит знакомить пользователей из документации, а не из fat-jar:

| Версия Vida | Что важно для автора/игрока (не в installer.jar, в runtime) |
|-------------|-----------------------------------------------------------|
| **0.5.0** | `vida-entidad`, `vida-mundo`, `@OyenteDeTick` — моды с сущностями/миром. |
| **0.6.0** | `vida-render`, `vida-red`, data-driven прототип, превью `@VifadaMulti` / `@VifadaLocal`. |
| **0.7.0** | Публичные моды `Saciedad`, `Senda` (пример `mods/` в репо). |
| **0.8.0** | Синтетические провайдеры `vida` / `minecraft` / `java` в резолвере; платформенные морфы `LatidoPulso` / `LatidoRenderHud`; версии через `BootOptions` / premain. |
| **0.9.0** | Мод **Valenta** (рендер, опционально) — `docs/mods/valenta/`. |

## CLI

Подробный справочник — [reference/cli-installer.md](../reference/cli-installer.md). Основные флаги:

- `--headless` — без GUI.
- `--launcher <kind>` — `minecraft` | `prism` | `multimc` | `atlauncher` | `modrinth` | `curseforge`.
- `--dir <path>` — директория лаунчера (для `detectDataDirs`).
- `--list-instances` — вывести список найденных инстансов и выйти.
- `--instance <path>` — конкретный инстанс (для ATLauncher и аналогичных PATCH-режимов).
- `--instance-name <string>` — имя нового инстанса (для Prism/MultiMC).
- `--minecraft <version>` — целевая версия MC (для 1.21.1 — основной путь тестов).
- `--loader-version <v>` — версия загрузчика Vida (по умолчанию — из встроенного агента).
- `--dry-run` — показать план, ничего не писать.
- `-y` / `--yes` — пропустить финальное подтверждение.
- `--validate-puertas <path>` *(0.3.0+)* — распарсить `.ptr` или обойти каталог с `.ptr` через `PuertaParser`. Exit-code: `0` — успех, `1` — неверные аргументы/путь, `2` — ошибка парсинга или I/O. Удобно в CI модов.

## GUI

Swing-окно с:

- выпадающим списком лаунчеров;
- полем «директория» с автодетектом и «обновить»;
- для Prism/MultiMC — поле «имя инстанса»;
- для ATLauncher — выпадающий список существующих инстансов + кнопка «обновить»;
- логом в реальном времени;
- кнопками «Установить», «Отмена», «Открыть README».

### `ModrinthHandler` *(0.4.0)*

Профили хранятся в SQLite (`app.db`), а не в файловой системе.

1. Читает `app.db` через JDBC для получения списка профилей.
2. Копирует loader.jar в `profiles/<id>/vida/`.
3. Обновляет `java_args` в `app.db` с `-javaagent`.
4. Пишет `install.json`.

Требуется SQLite JDBC (поставляется в составе installer fat-jar).

### `CurseForgeHandler` *(0.4.0)*

CurseForge App хранит инстансы в `Instances/<name>/` с `minecraftinstance.json`.

1. Читает `minecraftinstance.json` для получения списка инстансов.
2. Копирует loader.jar в `<instance>/vida/`.
3. Устанавливает `javaArgsOverride` в `minecraftinstance.json` (недокументированное, но стабильное поле).
4. Пишет `install.json`.

Как и ATLauncher — только PATCH_EXISTING_INSTANCE.

## Тесты

`installer/src/test/java` + integration-тесты с временными директориями:

- `PrismHandlerIT` — создание нового instance с нуля в temp-dir.
- `MultiMCHandlerIT` — то же, но с `JvmArgs`-инжектом.
- `ATLauncherHandlerIT` — патчинг существующего `instance.json`.
- `CliArgsLauncherTest` — парсинг всех флагов, включая специфичные для каждого лаунчера.
- `ATLauncherJsonPatcherTest` — контрактные тесты json-патчера.
- `PrismInstanceScannerTest` — скан папки с N-мя `instance.cfg`.
- `CurseForgeJsonPatcherTest` *(0.4.0)* — патчинг `javaArgsOverride`: добавление, замена, пустые значения.
- `CurseForgeInstanceScannerTest` *(0.4.0)* — сканирование папки `Instances/`.
- `ModrinthDbReaderTest` *(0.4.0)* — чтение/запись SQLite, патчинг `java_args`, edge cases.

## Что читать дальше

- [guides/multi-launcher.md](../guides/multi-launcher.md) — практические сценарии.
- [reference/cli-installer.md](../reference/cli-installer.md) — полный справочник CLI.
- [getting-started/installation.md](../getting-started/installation.md) — для игрока.

