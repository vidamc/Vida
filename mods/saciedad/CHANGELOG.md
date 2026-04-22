# Saciedad Changelog

## [1.0.0] — 2026-04-22

Первый публичный релиз мода.

### Added

- Шкала насыщения на HUD: два подписчика на `LatidoRenderHud` с `Fase.DESPUES`.
- `SaciedadConfig` — типизированная конфигурация (`color`, `mostrarSiempre`, `posicion`).
- `saciedad.toml` — файл конфигурации с поддержкой ARGB hex-цвета и трёх позиций.
- `FoodDataSaciedadMorph` — Vifada-морф, читающий `FoodData#saturation` через `@VifadaShadow`.
- `SaciedadCache` — безаллокационный кэш значения насыщения.
- `saciedad.ptr` — Puertas access-widener для `FoodData#saturation`.
- Производительность: < 0.1% CPU, 0 аллокаций на кадр после прогрева.
- Тесты: `SaciedadConfigTest` (парсинг конфига), `SaciedadHudRenderizadorTest` (headless GL-mock).
