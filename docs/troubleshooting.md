# Troubleshooting

Сборник проблем и решений, разбитый по фазе запуска. Если ни один пункт не помог — откройте [issue](https://github.com/vidamc/Vida/issues/new/choose) с приложенными `latest.log`, `crash-reports/*`, командной строкой запуска и выводом `java -version`.

## Инсталлятор

### «Exit code 1» при headless-установке

Проверьте, что `--minecraft` соответствует поддерживаемой версии (сейчас только `1.21.1`) и что `--dir` указывает на существующую папку лаунчера. Для Mojang Launcher это обычно `%APPDATA%/.minecraft`, для Prism — `%APPDATA%/PrismLauncher`, для ATLauncher — `%USERPROFILE%/ATLauncher`.

### GUI установщика не запускается в headless-системе

По умолчанию `java -jar vida-installer.jar` пытается поднять Swing. На headless-серверах добавьте `--headless`:

```bash
java -jar vida-installer.jar --headless --minecraft 1.21.1 -y
```

Полный список флагов — [reference/cli-installer.md](./reference/cli-installer.md).

### Prism/MultiMC — «instance не появляется»

Проверьте, что `instances/<name>/instance.cfg` и `instances/<name>/mmc-pack.json` были созданы. Если инсталляция прошла, но лаунчер не видит инстанс — перезапустите Prism/MultiMC: они кэшируют список инстансов при старте.

### ATLauncher — «path contains whitespace»

ATLauncher парсит `launcher.javaArguments` по пробелам и не умеет работать с quoted-путями. Инсталлятор намеренно отклоняет пути с пробелами. Перенесите ATLauncher или Vida-агент в путь без пробелов (например, `C:\ATLauncher` вместо `C:\Program Files\ATLauncher`).

## Запуск Minecraft

### `NoClassDefFoundError` / `ClassNotFoundException` на старте

Почти всегда — это «тонкий» JAR Vida-агента (без shaded-зависимостей). Убедитесь, что вы используете именно `vida-loader-<ver>-agent.jar`, собранный задачей `:loader:agentJar`, а не обычный `:loader:jar`. В релизных артефактах и инсталляторе — всегда shaded-вариант.

### Minecraft крашится с `VerifyError` после установки Vida

Это означает, что `Vifada` или `Escultor` сгенерировал невалидный байткод. Воспроизведите с опцией `-Dvida.debug.dumpClasses=true` — патченные классы сохранятся в `run/vida/dumps/`. Откройте issue и приложите эти файлы.

### Микро-фризы в геймплее

Если вы видите пики в frame-time каждые несколько секунд и они совпадают с логами Vida — это регрессия. Убедитесь, что в агентском манифесте стоит `Can-Retransform-Classes: false` (должно быть всегда). Если изменение своё — проверьте, что вы не регистрируете трансформер с `canRetransform=true`. См. [architecture/performance.md](./architecture/performance.md).

### F3 показывает `vida-<ver>/vanilla` вместо чистого `(vida)`

Это значит, что `BrandingEscultor` не отработал. Возможные причины:

1. Класс `net.minecraft.client.gui.components.DebugScreenOverlay` (или его обфусцированная версия) был загружен до старта агента. Убедитесь, что `-javaagent` стоит **первым** в JVM-аргументах.
2. Ваша версия Minecraft не 1.21.1 — байт-скан рассчитан на точную format-строку 1.21.1.

## Разработка мода

### `vidaGenerateManifest` валится с «mod id must not be blank»

В `build.gradle.kts` забыли `mod { id.set("...") }`. ID обязателен и должен соответствовать `[a-z0-9_.-]+`.

### Резолвер не находит зависимость

Проверьте, что:

1. Зависимость физически лежит в папке `mods/` dev-runtime.
2. `vida.mod.json` зависимости имеет корректный `id` (без namespace, без `:`).
3. Диапазон в `dependencies` совместим: `"^1.2"` допускает `1.2.x`–`1.9999.x`, `"~1.2"` — только `1.2.x`.

Алгоритм разрешения — [modules/resolver.md](./modules/resolver.md).

### `@VifadaInject` не срабатывает

1. Убедитесь, что класс с `@VifadaMorph` упомянут в `vida.mod.json` в секции `vifada.morphs`.
2. Проверьте сигнатуру целевого метода (`method = "name()V"`): формат — имя + JVM-дескриптор.
3. Последним параметром метода-инъектора должен быть `CallbackInfo`.

См. [guides/vifada.md](./guides/vifada.md).

### IDE не видит vanilla-классы

Нужно настроить Cartografía в Gradle:

```kotlin
vida {
    minecraft {
        version.set("1.21.1")
        mappings {
            proguard.set(file("mappings/mojang_1_21_1.txt"))
        }
    }
}
```

И запустить `./gradlew vidaRemapJar` или `./gradlew genSources`. См. [modules/gradle-plugin.md](./modules/gradle-plugin.md).

## Отладка

### Как включить подробный лог загрузчика

```bash
java -Dvida.log.level=DEBUG -javaagent:vida-loader.jar -jar ...
```

### Как подключить профайлер

Vida не ставит палки в колёса профайлерам. Передайте нужные агентские аргументы до или после Vida — порядок значения не имеет:

```bash
java -agentpath:/path/to/libyjpagent.so -javaagent:vida-loader.jar ...
```

Встроенный `Vigia` — [в планах](./roadmap.md).

### Как сохранить байты патченных классов

```bash
java -Dvida.debug.dumpClasses=true -javaagent:vida-loader.jar ...
```

Дампы окажутся в `run/vida/dumps/<package>/<class>.class`. Используйте `javap -p -v` или `asm-util` для чтения.
