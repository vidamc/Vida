# Установка Vida — для игрока

Vida ставится одним инсталлятором, который умеет работать со всеми поддерживаемыми лаунчерами Minecraft. Запустите GUI или используйте CLI — результат один.

## Предварительно

- **Minecraft Java Edition 1.21.1** — другая версия не поддерживается.
- **JDK 21 или новее.** Подойдёт любой OpenJDK 21+ (Temurin, Microsoft Build, Liberica, Zulu, Amazon Corretto). Лаунчер, подставляющий собственную Java 17, не подойдёт — потребуется явно указать Java 21.
- Свободные **200 МБ** на диске.

## Скачивание

[GitHub Releases](https://github.com/vidamc/Vida/releases/latest) — последняя стабильная сборка.

Каждый релиз содержит:


| Файл                        | Назначение                                                                             |
| --------------------------- | -------------------------------------------------------------------------------------- |
| `vida-installer-<ver>.jar`  | универсальный установщик, работает везде, где есть Java 21+                            |
| `vida-installer-<ver>.exe`  | Windows-лаунчер (если скачали через браузер — Windows может потребовать разблокировки) |
| `vida-installer-<ver>.msi`  | Windows MSI-установщик                                                                 |
| `vida-installer-<ver>.deb`  | Debian / Ubuntu                                                                        |
| `vida-installer-<ver>.dmg`  | macOS                                                                                  |
| `SHA256SUMS` / `SHA512SUMS` | контрольные суммы                                                                      |
| `*.sig` / `cosign.bundle`   | Sigstore-подписи (keyless)                                                             |


### Проверка подписи (рекомендуется)

```bash
cosign verify-blob \
  --certificate-identity-regexp='^https://github.com/vidamc/Vida/' \
  --certificate-oidc-issuer='https://token.actions.githubusercontent.com' \
  --bundle vida-installer-<ver>.jar.cosign.bundle \
  vida-installer-<ver>.jar
```

Проверка SHA:

```bash
sha256sum -c SHA256SUMS
```

## Установка через GUI

1. Дважды кликните `vida-installer-<ver>.jar` (или запустите через `java -jar`).
2. В выпадающем списке **Лаунчер** выберите свой: Mojang / Prism / MultiMC / ATLauncher.
3. Поле **Директория** заполняется автоматически (стандартный путь конкретной ОС). Если у вас нестандартный путь — поправьте.
4. Для Prism/MultiMC задайте **имя инстанса** (например, `Vida 1.21.1`). Для ATLauncher — выберите существующий инстанс из списка (кнопка «Обновить»).
5. Нажмите **Установить**.

Рядом с кнопкой появится лог. Зелёная галочка — инстанс готов, можно запускать.

## Установка через CLI

Headless-режим (никаких GUI-диалогов) — `--headless`. Флаг `-y` (или `--yes`) пропускает финальное подтверждение.

### Mojang Launcher

```bash
java -jar vida-installer.jar --headless --minecraft 1.21.1 -y
```

Создаёт профиль `Vida 1.21.1` в `launcher_profiles.json`. После установки в лаунчере появится новый профиль — выберите его в списке.

### Prism Launcher

```bash
java -jar vida-installer.jar --headless \
  --launcher prism \
  --dir "$APPDATA/PrismLauncher" \
  --minecraft 1.21.1 \
  --instance-name "Vida 1.21.1" \
  -y
```

Для Flatpak — укажите `--dir "$HOME/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"`.

### MultiMC

```bash
java -jar vida-installer.jar --headless \
  --launcher multimc \
  --dir "/path/to/MultiMC" \
  --minecraft 1.21.1 \
  -y
```

MultiMC портативен — директорию инсталлятор не угадывает, её нужно указать явно.

### ATLauncher

```bash
java -jar vida-installer.jar --headless \
  --launcher atlauncher \
  --dir "$USERPROFILE/ATLauncher" \
  --instance "$USERPROFILE/ATLauncher/instances/MyPack" \
  -y
```

Перед патчингом список инстансов:

```bash
java -jar vida-installer.jar --headless --launcher atlauncher --list-instances
```

### Modrinth App *(0.4.0+)*

Профили в SQLite. Укажите `--dir` на корень данных Modrinth (где лежит `app.db`), затем либо создайте инстанс через UI Modrinth и патчьте его путём из `--list-instances`, либо следуйте [modules/installer.md](../modules/installer.md) (`ModrinthHandler`).

```bash
# Пример: укажите --dir на каталог данных Modrinth (где app.db), см. --list-instances
java -jar vida-installer.jar --headless --launcher modrinth --dir "/path/to/modrinth-data" -y
```

Точный путь зависит от ОС и flatpak/нативной установки — сначала выполните `--list-instances` с явным `--dir`.

### CurseForge App *(0.4.0+)*

Инстансы в `Instances/<name>/` с `minecraftinstance.json`. Только патч существующего инстанса (как ATLauncher). См. [modules/installer.md](../modules/installer.md) (`CurseForgeHandler`).

```bash
java -jar vida-installer.jar --headless --launcher curseforge --dir "/path/to/CurseForge" --list-instances
```

### Проверка `.ptr` без установки *(0.3.0+)*

Для модов с Puertas (в т.ч. [Saciedad](https://github.com/vidamc/Vida) в репо):

```bash
java -jar vida-installer.jar --validate-puertas ./mods/saciedad/saciedad.ptr
```

Полный справочник CLI — [reference/cli-installer.md](../reference/cli-installer.md).

## После установки

1. Откройте лаунчер и выберите свежесозданный инстанс/профиль.
2. Запустите игру.
3. Откройте F3 — должна быть строка `Minecraft 1.21.1 (vida)`. Если так — всё хорошо.
4. Положите моды в папку `mods/` инстанса. Vida подхватит их при следующем запуске.

## Если что-то пошло не так

- Общие ошибки — [troubleshooting.md](../troubleshooting.md).
- Проблема не описана — [issue](https://github.com/vidamc/Vida/issues/new/choose) с логом и `java -version`.

