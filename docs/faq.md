# FAQ

### Зачем ещё один загрузчик модов?

У существующих загрузчиков три болевых точки: долгий старт, работа в hot-path игрового тика, устаревший API, поверх которого приходится строить неудобные абстракции. Vida спроектирована с нуля вокруг трёх решений — быстрый старт, нулевая работа после инициализации, API, который сам уводит нагрузку с главного потока. Подробнее — [architecture/performance.md](./architecture/performance.md).

### На какой версии Minecraft работает Vida?

Только **Java Edition 1.21.1** на момент этой документации. Стратегия поддержки новых версий — в [reference/api-stability.md](./reference/api-stability.md#поддержка-версий-minecraft). Обратная совместимость мод↔загрузчик: если мод не обращается к изменившимся vanilla-символам, он переживает апгрейд без перекомпиляции.

### Какая версия Java нужна?

**JDK 21 или новее.** 21 LTS — базовая; совместимость тестируется до текущей GA (26 на момент этого документа).

### Vida совместима с модами других загрузчиков?

Нет, и это сознательное решение. Слой эмуляции означал бы компромиссы по производительности и стабильности, ради которых Vida не имеет смысла. Авторы модов переносят код точечно — публичный API покрывает все типовые задачи. См. [reference/api-stability.md](./reference/api-stability.md#совместимость-с-другими-экосистемами).

### Почему испанские слова?

Vida — «жизнь» по-испански, и проект сознательно использует семантическое поле вокруг этого слова: Latidos (биение сердца, события), Catalogo (реестры), Ajustes (настройки), Susurro (шёпот, асинхронные задачи), Vigia (страж, профайлер). Это отличает словарь Vida от английских терминов, которые уже заняты другими системами моддинга.

### Почему агент не использует `retransform`?

`Can-Retransform-Classes: false` в манифесте и `canRetransform=false` при регистрации трансформера — сознательное решение: retransformation заставляет JVM держать дополнительные структуры данных, мешает C2-оптимизациям и обычно не нужен Vida (мы патчим классы только при первой загрузке). См. [architecture/performance.md](./architecture/performance.md#почему-нет-retransform).

### Что делает `BrandingEscultor`?

Переписывает format-строку F3-дебаг-экрана Minecraft — вместо `Minecraft 1.21.1 (vida-0.1.0-SNAPSHOT/vanilla)` показывается аккуратное `Minecraft 1.21.1 (vida)`. Это единственный пример непубличного `Escultor` в стандартной поставке. См. [guides/escultores.md](./guides/escultores.md).

### Можно ли установить Vida в сторонние лаунчеры?

Да. Мульти-лаунчер инсталлятор поддерживает:

- **Mojang Launcher** — создаёт профиль через `launcher_profiles.json`.
- **Prism Launcher** — создаёт новый instance с Vida в `mmc-pack.json` (использует `+agents` Prism).
- **MultiMC** — аналог Prism, но через `JvmArgs` (у MultiMC нет `+agents`).
- **ATLauncher** — патчит существующий instance, добавляя `-javaagent` в `launcher.javaArguments`.
- **Modrinth App** и **CurseForge App** — с **0.4.0**; см. [modules/installer.md](./modules/installer.md) и [getting-started/installation.md](./getting-started/installation.md).

Руководство — [guides/multi-launcher.md](./guides/multi-launcher.md).

### Vida шпионит за мной / ходит в сеть?

Нет. Загрузчик не делает ни одного сетевого запроса. Инсталлятор ходит в GitHub Releases только если вы явно указали `--download`; по умолчанию он работает с локальным артефактом. Моды могут делать что захотят — это часть [модели доверия](../SECURITY.md#модель-доверия).

### Как отправить баг-репорт?

[Issue](https://github.com/vidamc/Vida/issues/new/choose) с шаблоном `bug_report`. Для security-проблем — приватное [Advisory](https://github.com/vidamc/Vida/security/advisories/new).

### Как устроен релиз-процесс?

`release-please` автоматизирует релиз-PR на основе Conventional Commits. Пуш в `main` с `feat:` даёт MINOR-bump, с `fix:` — PATCH. Артефакты подписываются Sigstore (keyless) через `cosign`, прилагаются `SHA256SUMS` и `SHA512SUMS`. См. [CONTRIBUTING.md](../CONTRIBUTING.md#conventional-commits).

### Какая лицензия?

Apache-2.0. Коммерческое использование разрешено, требуется сохранение уведомления об авторстве и, при наличии, патентный грант. Полный текст — [`LICENSE`](../LICENSE).
