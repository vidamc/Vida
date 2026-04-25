# Матрица соответствия исходному ТЗ (vida.md)

Живой документ: при каждом релизе обновлять строки «Состояние» и «Проверка». Источник требований — [vida.md](../../vida.md) в корне репозитория.

| Раздел ТЗ | Требование | Состояние | Реализация / артефакт | Проверка |
|-----------|------------|-----------|----------------------|----------|
| CR §1 | Быстрый старт, параллельная дискавери, кэш | Реализовано + расширено | `discovery`, `TransformBytecodeCache`, параллельная подготовка провайдеров в `BootSequence` | `./gradlew test`, бенчмарк-харнесс см. [performance.md](../architecture/performance.md) |
| CR §1 | Нулевая работа лоадера в тике после инициализации | Реализовано | `VidaClassTransformer` hot-path, `canRetransform=false` | [classloading.md](../architecture/classloading.md), [performance.md](../architecture/performance.md) |
| CR §1 | Минимум RAM, ленивость | Частично | Ленивая инициализация API; лимиты кеша байткода | [performance.md](../architecture/performance.md) §Память |
| CR §1 | Многопоточный резолвер | Реализовано (безопасная стадия) | Параллельное построение `Provider` в `BootSequence`; бэктрекинг остаётся детерминированным | `ResolverParallelismTest`, `ResolverLargeUniverseTest` (300+ модов, CI) |
| CR §2 | Модульный API, SemVer, миграции | Реализовано | [api-stability.md](./api-stability.md), `CHANGELOG`, `docs/migration/*` | CI |
| Ч.1 §1 | ClassLoader, изоляция модов | Реализовано | `JuegoLoader`, `ModLoader` | [classloading.md](../architecture/classloading.md) |
| Ч.1 §1 | Ранний захват (VidaPremain) | Реализовано | `VidaPremain`, `VidaBoot` | [bootstrap.md](../architecture/bootstrap.md) |
| Ч.1 §1 | Cartografía | Реализовано | `cartografia`, Gradle remap | [cartografia.md](../modules/cartografia.md) |
| Ч.1 §1 | Профили конфигов / лаунчеры | Реализовано | `installer`, `BootOptions`, профили платформы | [installer.md](../modules/installer.md) |
| Ч.1 §2 | Скан `mods/`, nested JAR, манифест | Реализовано | `discovery`, `manifest` | Модульные тесты |
| Ч.1 §2 | Резолвер SemVer, конфликты | Реализовано | `resolver` SAT-бэктрекинг | [resolver.md](../modules/resolver.md) |
| Ч.1 §3 | Жизненный цикл мода (фазы) | Реализовано | `LatidoFaseCiclo`, порядок в `BootSequence.invokeEntrypoints` | `BootLifecyclePhasesTest` |
| Ч.2 | Vifada + приоритеты + отладка | Реализовано | `vifada`, `VifadaMorphTraceHtml` | `-Dvida.vifada.trace.html=…`, тест HTML |
| Ч.2 | Puertas | Реализовано | `puertas` | [puertas модуль](../modules/) |
| Ч.2 | Escultores | Реализовано | `escultores` | [escultores.md](../modules/escultores.md) |
| Ч.3 | Latidos, Catalogo, Tejido, Fuente, Ajustes | Реализовано + расширено | `base`, `red`, `fuente`, фрагментация Tejido | Модульные тесты |
| Ч.3 | Tejido: крупные пакеты | Реализовано | `TejidoFragmentacion`, `TejidoEnsambladorFragmentos` | `TejidoFragmentacionTest` |
| Ч.3 | Ajustes: перезагрузка + синхронизация | Реализовано | `LatidoConfiguracionRecargada`, `Ajuste.sincronizar()`, `PaqueteAjustesSincronizacionServidor` | Тесты `red`, `ClientResourceReloadMorph` |
| Ч.3 | Fuente: перезагрузка ресурсов | Реализовано | `LatidoFuenteRecargada`, `FuenteRecarga` | Док [fuente.md](../modules/fuente.md), тест |
| Ч.4 | Susurro, Latidos profundos | Реализовано | `susurro`, `Ejecutor` в `base` | [base.md](../modules/base.md) |
| Ч.4 | Vigia | Реализовано | `vigia` | [модуль vigia](../modules/) |
| Ч.5 | Gradle plugin, доки, vidaDocTest | Реализовано | `gradle-plugin`, `docs/` | `./gradlew vidaDocTest` |
| Ч.6 | Версии MC, без чужой бинарной совместимости | Политика зафиксирована | [api-stability.md](./api-stability.md) | — |
| Ч.доп | Безопасность модов (ModuleLayer) | Зафиксировано в дизайне | Изоляция через classloader; JPMS — см. [classloading.md](../architecture/classloading.md) §JPMS | `ModLoaderIsolationTest` |
| «Все аспекты игры» | Полный игровой API | Волнами | [domain-gap.md](./domain-gap.md) — закрыто / в плане / вне объёма | Roadmap 2.x+ |

## Процесс при релизе

1. Пройти таблицу сверху вниз; обновить ссылки на тесты и документы.
2. Если требование сменило статус — запись в `CHANGELOG.md` под соответствующую версию.
3. Запустить `./gradlew build test vidaDocTest` (или эквивалент CI).

## Не входит в объём «одного репозитория Vida»

- Формальное сравнение «на N % быстрее всех сторонних лоадеров» без выбранных эталонов и методики — заменено измеримыми бюджетами в README и performance.md.
