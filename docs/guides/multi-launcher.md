# Мульти-лаунчер инсталлятор

Как Vida интегрируется с четырьмя лаунчерами и что делать, если вы хотите добавить свой.

Обзор модуля — в [`modules/installer.md`](../modules/installer.md). Для игрока — [`getting-started/installation.md`](../getting-started/installation.md). CLI-справка — [`reference/cli-installer.md`](../reference/cli-installer.md).

## Что именно делает инсталлятор

Неважно, какой лаунчер — цель одна: запустить Minecraft 1.21.1 с `-javaagent:vida-loader.jar` в командной строке JVM. Каждый лаунчер предлагает свой механизм:

| Лаунчер | Механизм инжекта `-javaagent` |
|---------|-------------------------------|
| Mojang  | `javaArgs` в профиле `launcher_profiles.json` |
| Prism   | `+agents` в компонент-патче `patches/dev.vida.loader.json` |
| MultiMC | `JvmArgs` в `instance.cfg` (нет `+agents`) |
| ATLauncher | `launcher.javaArguments` в `instance.json` |

Плюс каждый требует свою файловую структуру вокруг: где лежит JAR агента, как связан с версией Minecraft, где хранить данные.

## Mojang Launcher

### Структура профиля

```
%APPDATA%/.minecraft/
    launcher_profiles.json       ← список профилей
    versions/
        vida-1.21.1/             ← наш профиль-версия
            vida-1.21.1.json     ← манифест запуска
        1.21.1/                  ← vanilla (наследуемся)
```

`vida-1.21.1.json`:

```json
{
  "id": "vida-1.21.1",
  "inheritsFrom": "1.21.1",
  "type": "release",
  "mainClass": "net.minecraft.client.main.Main",
  "arguments": {
    "jvm": ["-javaagent:${library_directory}/vida-loader.jar"]
  },
  "libraries": [
    {
      "name": "dev.vida:vida-loader:0.1.0",
      "downloads": { "artifact": { "path": "dev/vida/vida-loader/0.1.0/vida-loader.jar" } }
    }
  ]
}
```

### `launcher_profiles.json`

Добавляем запись:

```json
{
  "profiles": {
    "vida": {
      "name": "Vida 1.21.1",
      "type": "custom",
      "lastVersionId": "vida-1.21.1",
      "created": "...",
      "icon": "..."
    }
  }
}
```

Atomic write (`InstallerSupport.writeAtomically`): пишем в `launcher_profiles.json.tmp`, потом rename — если крашнемся, файл не потеряется.

## Prism Launcher

### Структура инстанса

```
instances/<name>/
    instance.cfg                 ← INI-конфиг
    mmc-pack.json                ← список компонентов
    patches/
        dev.vida.loader.json     ← наш компонент-патч
    libraries/
        vida-loader-0.1.0.jar    ← shaded агент
    .minecraft/                  ← рабочая директория игры
```

### `instance.cfg`

```ini
InstanceType=OneSix
name=Vida 1.21.1
iconKey=default
OverrideJavaArgs=true
JvmArgs=-Xmx4G
```

### `mmc-pack.json`

```json
{
  "components": [
    { "uid": "net.minecraft", "version": "1.21.1" },
    {
      "uid": "dev.vida.loader",
      "version": "0.1.0",
      "cachedRequires": [{ "uid": "net.minecraft", "equals": "1.21.1" }]
    }
  ],
  "formatVersion": 1
}
```

### `patches/dev.vida.loader.json`

Современный Prism (≥ 8.x):

```json
{
  "formatVersion": 1,
  "name": "Vida Loader",
  "uid": "dev.vida.loader",
  "version": "0.1.0",
  "requires": [{ "uid": "net.minecraft", "equals": "1.21.1" }],
  "compatibleJavaMajors": [21, 22, 23, 24, 25],
  "+agents": [
    {
      "name": "dev.vida:vida-loader:0.1.0",
      "url": "",
      "path": "libraries/vida-loader-0.1.0.jar"
    }
  ]
}
```

`+agents` — декларативная фича Prism: лаунчер сам добавит `-javaagent` в JVM-строку.

## MultiMC

Почти идентично Prism, но без `+agents`:

### `instance.cfg`

```ini
InstanceType=OneSix
name=Vida 1.21.1
OverrideJavaArgs=true
JvmArgs=-javaagent:instance/libraries/vida-loader-0.1.0.jar -Xmx4G
```

### `patches/dev.vida.loader.json`

```json
{
  "formatVersion": 1,
  "uid": "dev.vida.loader",
  "version": "0.1.0",
  "requires": [{ "uid": "net.minecraft", "equals": "1.21.1" }],
  "+mavenFiles": [
    {
      "name": "dev.vida:vida-loader:0.1.0",
      "path": "libraries/vida-loader-0.1.0.jar"
    }
  ]
}
```

Это обычное maven-файл заявление; инжект `-javaagent` — через `JvmArgs` в `instance.cfg`.

## ATLauncher

Другая архитектура — нет `patches/`, всё в одном `instance.json`:

```json
{
  "launcher": {
    "pack": "MyPack",
    "version": "1.0.0",
    "mainClass": "net.minecraft.client.main.Main",
    "javaArguments": "-Xmx4G -javaagent:vida/vida-loader-0.1.0.jar",
    ...
  },
  "libraries": [...]
}
```

### Особенности

1. **Только PATCH_EXISTING_INSTANCE.** ATLauncher не терпит «чужих» инстансов, созданных не им — они не появятся в UI. Мы инструктируем игрока создать инстанс под нужный модпак ATLauncher'ом, а затем запатчить его.

2. **Пути без пробелов.** ATLauncher парсит `javaArguments` по пробелам без quoting'а. Путь `C:\Program Files\ATLauncher\...` с пробелами ломает команду запуска. Патчер явно валидирует и отклоняет.

3. **Атомарная замена.** `instance.json` — это весь конфиг инстанса. Любая ошибка записи → игрок теряет модпак. Поэтому `InstallerSupport.writeAtomically`.

4. **Дедупликация агентов.** При переустановке Vida мы находим старый `-javaagent:.../vida-*.jar` (regex) и заменяем его — чтобы не копился список.

### Директория агента

Агент копируется в `<instance>/vida/vida-loader-<ver>.jar`. Относительный путь в `javaArguments`:

```
-javaagent:vida/vida-loader-0.1.0.jar
```

ATLauncher разрешает относительные пути от рабочей директории инстанса.

## Modrinth App и CurseForge App *(0.4.0+)*

Реализовано в `ModrinthHandler` и `CurseForgeHandler` (см. [modules/installer.md](../modules/installer.md)).

### Modrinth

- Профили в SQLite `app.db`; чтение через `ModrinthDbReader`, JDBC **sqlite-jdbc** в fat-jar инсталлятора.
- Патч `java_args` / профилей в БД, копирование `vida-loader` в согласованный путь.

### CurseForge

- Скан `Instances/`, `minecraftinstance.json`, поле `javaArgsOverride` через `CurseForgeJsonPatcher` / `CurseForgeInstanceScanner`.

CLI: `--launcher modrinth` / `--launcher curseforge` и тот же набор флагов, что у остальных (часто нужен явный `--dir`). Детали сценариев — [getting-started/installation.md](../getting-started/installation.md).

## Как добавить свой лаунчер

Если у вас есть launcher, который не покрыт — добавление сводится к:

1. **Определите `LauncherKind`** в enum (`dev.vida.installer.launchers.LauncherKind`).
2. **Реализуйте `LauncherHandler`** (`dev.vida.installer.launchers.LauncherHandler`). Минимум:
   - `detectDataDirs()` — где искать.
   - `listInstances(...)` — как сканить.
   - `install(...)` — главная процедура.
3. **Зарегистрируйте** в `LauncherRegistry` (`dev.vida.installer.launchers.LauncherRegistry`).
4. **Добавьте integration-тест** с временной директорией (стиль `PrismHandlerIT`).
5. **Документируйте** в `docs/getting-started/installation.md` + этой странице.

Для контрибьюции — [`CONTRIBUTING.md`](../../CONTRIBUTING.md).

## Что читать дальше

- [modules/installer.md](../modules/installer.md) — архитектура модуля.
- [reference/cli-installer.md](../reference/cli-installer.md) — все CLI-флаги.
- [getting-started/installation.md](../getting-started/installation.md) — руководство игрока.
