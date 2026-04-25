# cima

**Cima** (исп. «верх», «сюммит») — публичный слой **прямого** доступа к <strong>тому же</strong> <strong>vanilla-уровню</strong>, с которого загрузчик строит `MundoNivelVanilla` и <strong>LatidosMundo.Tick</strong>, <strong>помимо</strong> намеренно абстрактного API <strong>vida-mundo</strong> ([mundo](mundo.md)): без дублирования «витрин» `ChunkMap` / шума в <strong>vida-mundo</strong>, зато <strong>явно</strong> в руках кастомных модов.

- **Пакет:** `dev.vida.cima`
- **Артефакт:** `dev.vida:cima` (как к `dev.vida:vida-bom`, [platform-bom](../reference/platform-bom.md))
- **Стабильность:** <strong>Preview</strong> (`@ApiStatus.Preview("cima")`) — контракт будет расширяться; привязка к Mojang остаётся <strong>волатильной</strong> (отдельно от SemVer cima).

## Что внутри

| Тип / метод | Смысл |
|-------------|--------|
| <strong><code>CimaJuego</code></strong> | `vinculado()` — на клиенте `Minecraft#getInstance()#getLevel() != null`; <strong><code>nivelMinecraftVivo</code></strong> — <strong>Object = net.minecraft.world.level.Level</strong> (лёгкое <strong>escape-hatch</strong>); <strong><code>mundoSobreNivelCargado</code></strong> — тот же <strong><code>dev.vida.mundo.Mundo</code></strong>, что в bridge (обёртка, как <code>new MundoNivelVanilla(level)</code>). |
| <strong><code>CimaJuegoGlobal#cimaJuego()</code></strong> | Синглтон, как <strong>шина</strong> <code>LatidoGlobal</strong>: <strong>не</strong> в <code>ModContext</code>, потому что <code>:base</code> остаётся тонким. До <strong>:loader</strong> — <code>CimaJuegoNulo</code>. |
| <strong>Реализация <code>:loader</code></strong> | <code>dev.vida.platform.CimaJuegoCarga</code> (internal), вешается в <code>BootSequence</code> сразу после <strong>PlatformBridge</strong>. |

## Зависимости Gradle (мод)

```kotlin
compileOnly(platform("dev.vida:vida-bom:…"))
compileOnly("dev.vida:cima")
compileOnly("dev.vida:base")
compileOnly("dev.vida:mundo")
```

Далее, для настоящей работы с <strong>Level</strong> — как обычно, классы <strong>net.minecraft.*</strong> на <strong>игровом</strong> classloader, [Cartografía](cartografia.md) / <strong>mc-stabs</strong> / client JAR в Gradle; при необходимости — <strong>[Vifada](vifada.md)</strong>, <strong>[Puertas](puertas.md)</strong>, [classloading](../architecture/classloading.md).

## Чего cima НЕ обещает

- Не заменяет <strong>полноценный</strong> фасад <strong>всего</strong> движка (тот же <strong>noise / chunk / BE</strong>); это по-прежнему <strong>ваш</strong> код + мост, либо новые <strong>Preview</strong>-разделы cima (roadmap, после обратной связи).
- На <strong>выделенном</strong> сервере: текущий <code>CimaJuegoCarga</code> смотрит на <strong>клиентский</strong> <code>Minecraft#level</code>; для <code>ServerLevel</code> — план: отдельные методы/реализация (тот же слой, другой вход).

## Связь с другим API

- [mundo](mundo.md) — <strong>абстрактный</strong> <strong><code>Mundo</code></strong>, Latidos.
- [loader](loader.md) — bootstrap, <strong>PlatformBridge</strong>, <strong>MundoNivelVanilla</strong>.
