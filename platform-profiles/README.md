# Platform profiles

Canonical descriptors for **Vida platform profiles**: one folder per supported Minecraft drop, grouped by generation (`legacy-121`, `calendar-26`, …).

Each drop directory contains `profile.json` validated against `schema/platform-profile.schema.json` and by the Gradle task `verifyPlatformProfiles` (required keys + folder/`generation` consistency).

## Fields (summary)

- **`gameVersion`** — строка дропа Minecraft.
- **`generation`** — `LEGACY_121` или `CALENDAR_26`.
- **`mappings.strategy`** — `GAME_DIR_PROGUARD` (лаунчер `.minecraft/versions/<ver>/`) или `CLASSPATH_PROGUARD` (+ `classpathResource`).
- **`mappingMode`** — opaque tag for Cartografía policy (reserved).
- **`platformBridge`** — FQCN реализации `PlatformBridge` (по умолчанию `dev.vida.platform.VanillaBridge`).
- **`morphBundle`** — список FQCN платформенных морфов из jar загрузчика; если ключ **отсутствует**, регистрируются все встроенные платформенные морфы.
- **`minimumJavaVersion`**, **`recommendedJavaVersion`**, **`dataPackFormat`**, **`resourcePackFormat`** — опционально.
- **`clientJar.sha256`** — для будущих контрактных проверок артефакта.

## Layout

```
generations/<generation-folder>/<game-drop>/profile.json
```

Heavy artifacts (client jar) stay **out** of Git; record SHA-256 when enforcing checksums.

## Runtime

The `:loader` module copies `generations/**` into `META-INF/vida/platform-profiles/` on the classpath.

Select a profile with:

1. `BootOptions.platformProfileId`, or  
2. JVM `-Dvida.platformProfile=<id>` (e.g. `legacy-121/1.21.7`).

Gradle `vida.minecraft.platformProfile` / convention from `version` — see [docs/modules/platform-profiles.md](../docs/modules/platform-profiles.md).

## Mod compatibility

Mods may declare compatible profile ids under `custom.vida.platformProfileIds` in `vida.mod.json` when an active platform profile is set.
