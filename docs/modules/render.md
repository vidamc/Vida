# render

Публичный render API Vida для клиентских модов: декларативные модели блоков и сущностей, texture-atlas и shader hooks. Модуль намеренно не тянет vanilla-клиент на compile classpath, поэтому его можно использовать и тестировать обычными unit-тестами.

- Пакет: `dev.vida.render`
- Gradle: `dev.vida:vida-render`
- Стабильность: `@ApiStatus.Preview("render")`

## Что есть в 0.6.0

### `ModeloBloque`

Описание render-модели блока: `geometria` + `texturaPrincipal`.

- Безопасный дефолт: `ModeloBloque.POR_DEFECTO` = `vida:cube` + `vida:missing_texture`.
- Shortcut: `ModeloBloque.cubo(textureId)` для стандартных кубических блоков.

### `ModeloEntidad`

Описание render-модели сущности: `malla` + `texturaPrincipal`.

- Дефолт: `ModeloEntidad.POR_DEFECTO` = `vida:entity/simple` + `vida:missing_texture`.
- Shortcut: `ModeloEntidad.simple(textureId)`.

### `TextureAtlas`

Иммутабельный snapshot зарегистрированных текстур.

- `TextureAtlas.builder()` — регистрация id текстур.
- `texturaMissing(...)` — явный missing-texture.
- `resolver(id)` — возвращает `id` или missing-texture, если текстура отсутствует.

### `RenderPipeline`

Центральная точка render API:

- регистрация моделей блоков: `registrarModeloBloque(...)`;
- регистрация моделей сущностей: `registrarModeloEntidad(...)`;
- регистрация shader hooks по этапам: `registrarHook(...)`;
- запуск hooks этапа: `ejecutarHooks(...)`.

Контракт безопасного поведения:

- если модель блока не зарегистрирована — используется `cube + missing-texture`;
- если модель сущности не зарегистрирована — используется `entity/simple + missing-texture`;
- если у зарегистрированной модели текстура отсутствует в atlas — она нормализуется в missing-texture.

### `ShaderHook` + `ContextoShader`

Pipeline-хук для пользовательского шейдерного кода:

- этапы `ANTES_MUNDO`, `DESPUES_MUNDO`, `HUD`;
- контекст вызова содержит `frameId`, `tiempoNanos`, `etapa`.

## Потокобезопасность

- `ModeloBloque`, `ModeloEntidad`, `TextureAtlas`, `ContextoShader` — иммутабельные.
- `RenderPipeline` рассчитан на конфигурацию на старте и дальнейшее чтение в рантайме.

## Тесты

`render/src/test/java`:

- `RenderPipelineTest` — fallback-поведение, нормализация текстур, порядок вызова hooks.
- `TextureAtlasTest` — поведение missing-texture и builder-контракт.

## Что читать дальше

- [`bloque`](./bloque.md) — API блоков, которые рендерятся через pipeline.
- [`entidad`](./entidad.md) — API сущностей и их декларативные свойства.
- [session-roadmap](../session-roadmap.md#session-4--060--рендер-сеть-дата) — общий план Session 4.
