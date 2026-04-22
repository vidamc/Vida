# Senda Changelog

## [1.0.0] — 2026-04-22

Первый публичный релиз мода.

### Added

- `PuntoRuta` — record путевой точки с координатами и измерением.
- `SendaCatalogo` — реестр точек поверх `CatalogoManejador` + внутренний мап; поддерживает регистрацию, поиск, удаление, очистку измерения.
- `SendaLatidos` — события `PuntoAgregado`, `PuntoEliminado`, `DimensionLimpiada`.
- `SendaConfig` — типизированная конфигурация (`maxPuntosPorDimension`, `dimensionInicial`).
- `senda.toml` — файл конфигурации с валидацией диапазонов и допустимых измерений.
- `SendaMod` — entrypoint, подписка на события через `LatidoRegistrador`.
- Тесты: `SendaConfigTest` (парсинг, валидация), `SendaCatalogoTest` (CRUD, события, лимиты).
