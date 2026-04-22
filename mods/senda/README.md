# Senda

**Senda** — навигационный мод для Vida. Доказательство концепции: демонстрирует `Catalogo` + `Ajustes` + `Latidos`.

## Возможности

- Сохранение именованных путевых точек (waypoints) per измерение
- Поиск и удаление точек
- Кастомные события (`SendaLatidos`) — другие моды могут реагировать на изменения
- Лимит точек на измерение настраивается через `senda.toml`

## Установка

Скопируйте JAR мода в папку `mods/` вместе с [Vida](https://github.com/vida-project/vida) ≥ 0.7.0.

## Конфигурация

```toml
# senda.toml

# Максимальное число путевых точек на измерение (1–1000)
maxPuntosPorDimension = 50

# Измерение по умолчанию при открытии HUD: overworld | nether | end
dimensionInicial = "overworld"
```

## API для других модов

```java
// Подписка на событие добавления точки
bus.suscribir(SendaLatidos.PuntoAgregado.TIPO, evento -> {
    PuntoRuta punto = evento.punto();
    // punto.nombre(), punto.dimension(), punto.x(), punto.y(), punto.z()
});
```

## Архитектура

| Компонент | Роль |
|---|---|
| `SendaMod` | Entrypoint, инициализация |
| `SendaCatalogo` | Управление точками (Catalogo + внутренний мап) |
| `SendaConfig` | Типизированная конфигурация (Ajustes) |
| `SendaLatidos` | Кастомные события (Latidos) |
| `PuntoRuta` | Record путевой точки |

## Сборка

```bash
./gradlew :mods:senda:build
```

## Лицензия

Apache-2.0 © 2026 Vida Project Authors
