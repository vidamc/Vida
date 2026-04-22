# Saciedad

**Saciedad** — мод для Vida, показывающий скрытую шкалу насыщения (*saturation*) поверх стандартной шкалы голода в HUD Minecraft 1.21.1.

## Установка

Скопируйте JAR мода в папку `mods/` вашего Minecraft-профиля вместе с [Vida](https://github.com/vida-project/vida) ≥ 0.7.0.

## Конфигурация

Файл `saciedad.toml` создаётся автоматически при первом запуске в папке с данными мода:

```toml
# Цвет заливки шкалы в hex ARGB (rrggbb или aarrggbb)
color = "FFFFA500"

# Показывать шкалу даже при нулевом насыщении
mostrarSiempre = false

# Позиция: arriba | abajo | encima
posicion = "abajo"
```

| Параметр | Тип | Умолчание | Описание |
|---|---|---|---|
| `color` | string (hex ARGB) | `FFFFA500` | Янтарный |
| `mostrarSiempre` | boolean | `false` | Показывать при нуле |
| `posicion` | string | `abajo` | Под / над / поверх шкалы голода |

## Производительность

- < 0.1% CPU
- 0 аллокаций на кадр после прогрева
- Подписка на `LatidoRenderHud` с `Fase.DESPUES` (поверх стандартного HUD)

## Как это работает

1. **`FoodDataSaciedadMorph`** — Vifada-морф, инжектируемый в `FoodData#tick`. При каждом тике обновляет `SaciedadCache.saturation` через `@VifadaShadow`.
2. **`SaciedadHudRenderizador`** — два подписчика на `LatidoRenderHud`: один рисует фон, второй — цветную шкалу шириной, пропорциональной насыщению.
3. **`saciedad.ptr`** — Puertas access-widener для поля `FoodData#saturation`.

## Сборка

```bash
./gradlew :mods:saciedad:build
```

## Лицензия

Apache-2.0 © 2026 Vida Project Authors
