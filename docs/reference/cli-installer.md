# CLI установщика Vida

Полный справочник флагов `vida-installer.jar`. Для пошаговых сценариев — [`getting-started/installation.md`](../getting-started/installation.md).

## Общий синтаксис

```bash
java -jar vida-installer.jar [ОПЦИИ]
```

Если ни одной опции не задано (и стоит Swing GUI) — запускается GUI. Любая из опций, отмеченных ниже как «CLI-mode», форсирует headless-режим без GUI.

## Флаги

### `--headless` — CLI-mode

Принудительно запустить без GUI. Обязательна, если вы хотите передать другие аргументы headless-скриптом, но не хотите форсировать это через environment (`VIDA_HEADLESS=1`).

### `--help`, `-h`

Вывести справку и выйти.

### `--version`, `-V`

Вывести строку `vida-installer <версия>` (версия fat-jar инсталлятора) и выйти. Версия агента совпадает с полем `--loader-version` по умолчанию при установке.

### `--list-instances` — CLI-mode

Найти и вывести существующие instance'ы выбранного `--launcher`. Не устанавливает ничего, только перечисляет:

```bash
java -jar vida-installer.jar --headless --launcher prism --list-instances
```

Вывод (для Prism):

```
Найдено инстансов: 3
  [1.21.1] Vida 1.21.1        — C:\Users\ana\AppData\Roaming\PrismLauncher\instances\Vida 1.21.1
  [1.21.1] Vanilla            — C:\Users\ana\AppData\Roaming\PrismLauncher\instances\Vanilla
  [1.20.6] Old Pack           — C:\Users\ana\AppData\Roaming\PrismLauncher\instances\Old Pack
```

Требуется `--launcher`. Можно указать `--dir` для переопределения пути.

### `--launcher <kind>` — CLI-mode

Выбор целевого лаунчера. Допустимые значения (с 0.4.0 реализованы все шесть):

- `mojang` / `minecraft` — Mojang Launcher (дефолт, если не указано).
- `prism` — Prism Launcher.
- `multimc` — MultiMC.
- `atlauncher` — ATLauncher.
- `modrinth` — Modrinth App (SQLite `app.db`, см. [modules/installer.md](../modules/installer.md)).
- `curseforge` — CurseForge App (`minecraftinstance.json`, `javaArgsOverride`).

### `--dir <path>` — CLI-mode

Директория данных лаунчера. Если не указана — инсталлятор пробует стандартные пути для выбранного `--launcher`:

- `mojang`: `%APPDATA%/.minecraft`, `~/Library/Application Support/minecraft`, `~/.minecraft`
- `prism`: `%APPDATA%/PrismLauncher`, `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher`, etc.
- `multimc`: **не угадывается** — MultiMC портативный, путь обязателен.
- `atlauncher`: `%USERPROFILE%/ATLauncher`, `~/Library/ATLauncher`, etc.

### `--instance <path>` — CLI-mode

Путь к конкретному инстансу. Обязателен для `atlauncher` (там только PATCH-режим). Для других лаунчеров — опционален (если указан, будет запатчен этот инстанс, иначе создан новый).

### `--instance-name <string>` — CLI-mode

Имя нового инстанса. Применяется к Prism / MultiMC при создании. По умолчанию: `Vida <minecraft-version>`.

### `--minecraft <version>` — CLI-mode

Версия Minecraft. На 0.x поддерживается только `1.21.1`. Другие версии — `ERROR`.

### `--loader-version <v>` — CLI-mode

Версия загрузчика Vida, попадает в `InstallOptions` и в итоговые JSON-патчи. По умолчанию — тег, зашитый в fat-jar; переопределение — для тестов и отката.

### `--dry-run`

Показать план, ничего не писать на диск. Полезно для проверки, что инсталлятор найдёт правильные пути.

### `--yes`, `-y`

Пропустить финальное подтверждение. Обязательно для полностью headless-сценариев (CI, автоустановка).

### `--overwrite`

Разрешить перезапись существующих файлов (например, `mmc-pack.json`, если он уже есть). Без этого флага инсталлятор остановится с `ERROR: target exists`.

### `--no-launcher-profile`

Не создавать профиль в `launcher_profiles.json` (только для `--launcher mojang`). Полезно для dev-сценариев, когда вы вручную управляете профилями.

### `--no-launch-script`

Не создавать `run.sh` / `run.bat` обёртку (только для standalone-установки без лаунчера).



### `--validate-puertas <path>` — CLI-mode *(0.3.0+)*

Распарсить один `.ptr` или обойти каталог (рекурсивно, `Files.walk`) и проверить все `.ptr` через `PuertaParser`. Лаунчер и `--dir` не нужны.

```bash
java -jar vida-installer.jar --validate-puertas ./src/main/resources/miaventura.ptr
java -jar vida-installer.jar --validate-puertas ./src/main/resources/
```

Вывод при ошибке:

```
src/main/resources/miaventura.ptr:12: unknown namespace 'interno' (allowed: crudo, intermedio, exterior)
src/main/resources/miaventura.ptr:17: duplicate directive for net/minecraft/world/Level.seed:J
```

Exit-code (см. `CliInstaller#validatePuertas`):

- `0` — все файлы валидны;
- `1` — аргументы/путь (например, путь не существует);
- `2` — хотя бы в одном файле ошибка парсинга (`PuertaError`) или I/O при обходе.

Удобно подключать в CI модов — парсер используется тот же, что во время сборки и рантайма, поэтому результат не разъезжается.

Подробно о формате: [modules/puertas.md](../modules/puertas.md) и [guides/puertas.md](../guides/puertas.md).

## Системные свойства и env

Эквиваленты CLI-флагов (для случаев, когда переопределить через env удобнее):

| Env | Аналог |
|-----|--------|
| `VIDA_INSTALLER_HEADLESS=1` | `--headless` |
| `VIDA_INSTALLER_LAUNCHER=prism` | `--launcher prism` |
| `VIDA_INSTALLER_DIR=...` | `--dir ...` |
| `VIDA_INSTALLER_YES=1` | `--yes` |

Приоритет: CLI-флаги > системные свойства > env > дефолты.

## Примеры

### Mojang Launcher, тихая установка

```bash
java -jar vida-installer.jar --headless --minecraft 1.21.1 -y
```

### Prism — новый instance с кастомным именем

```bash
java -jar vida-installer.jar --headless \
  --launcher prism \
  --dir "$APPDATA/PrismLauncher" \
  --instance-name "Vida 1.21.1 (prod)" \
  --minecraft 1.21.1 -y
```

### ATLauncher — патч существующего

```bash
# Сначала найти instance
java -jar vida-installer.jar --headless --launcher atlauncher --list-instances

# Потом патч
java -jar vida-installer.jar --headless \
  --launcher atlauncher \
  --dir "$USERPROFILE/ATLauncher" \
  --instance "$USERPROFILE/ATLauncher/instances/MyPack" \
  --minecraft 1.21.1 -y
```

### Dry-run в CI

```bash
java -jar vida-installer.jar --headless \
  --launcher prism \
  --dir "./test-prism" \
  --minecraft 1.21.1 \
  --dry-run
```

## Коды выхода

| Код | Значение |
|-----|----------|
| `0` | Успех |
| `1` | Ошибка валидации аргументов |
| `2` | Ошибка ввода-вывода (путь не существует, нет прав) |
| `3` | Конфликт (target exists, нужен `--overwrite`) |
| `4` | Невалидный лаунчер-state (например, поврежденный `launcher_profiles.json`) |
| `5` | Отменено пользователем (без `-y` и отрицательный ответ) |
| `64` | Некорректная команда (не CLI-флаг) |

## См. также

- [getting-started/installation.md](../getting-started/installation.md) — руководство игрока.
- [guides/multi-launcher.md](../guides/multi-launcher.md) — про-уровень, как устроено внутри.
- [modules/installer.md](../modules/installer.md) — архитектура модуля.
