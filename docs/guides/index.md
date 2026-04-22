# Guides

Практические материалы по конкретным API. В отличие от [модулей](../modules/index.md), где мы описываем _что_ есть, здесь мы показываем _как_ использовать.

## Для мод-автора

- [Latidos — события](./latidos.md) — подписки, приоритеты, фазы, отмена, `Ejecutor` (latidos profundos), создание своих событий.
- [Catalogo — реестры контента](./catalogo.md) — регистрация блоков/предметов/звуков, `CatalogoClave`, заморозка.
- [Первая сущность](./first-entity.md) — `vida-entidad`, `vida-mundo`, `@OyenteDeTick`, world-латидосы.
- [Ajustes — конфиги](./ajustes.md) — типизированные настройки, профили, синхронизация.
- [Vifada — модификация байткода](./vifada.md) — `@VifadaMorph`, `@VifadaInject`, `@VifadaShadow` на реальных примерах.
- [Puertas — access wideners](./puertas.md) — `.ptr`-файлы, расширение доступа к приватным полям и финальным классам.

## Для мод-инженера

- [Escultores — низкоуровневые трансформеры](./escultores.md) — когда Vifada недостаточно; на примере `BrandingEscultor`.
- [Мульти-лаунчер инсталлятор](./multi-launcher.md) — внутренняя логика, как добавить свой лаунчер, что знать про ATLauncher/Prism/MultiMC.
