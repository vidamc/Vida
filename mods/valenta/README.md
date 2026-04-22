# Valenta

**Sodium-class rendering optimization mod for [Vida](../../README.md) / Minecraft 1.21.1.**

Valenta replaces the vanilla rendering pipeline with a modern OpenGL 4.3+ backend,
achieving significant FPS improvements through batched draw calls, compact vertex
data, parallel chunk meshing, and multi-tier culling.

## Features

### Render Core
- **VBO Batcher** — groups all visible chunk sections into a single VBO and issues
  one `glMultiDrawElementsIndirect` call per render pass, eliminating per-section
  GL driver overhead.
- **Compact Vertex Format** — 16-byte vertices instead of vanilla's 28 bytes (~43%
  VRAM bandwidth reduction, better L2 cache utilisation on the GPU).
- **SSBO-backed data** — biome blending colors and block light values are stored in
  Shader Storage Buffer Objects, enabling smooth per-block interpolation in the
  fragment shader without additional draw calls.

### Chunk Meshing
- **Susurro task-graph** — three-stage pipeline (`Analisis → Build → Upload`)
  running on the shared `Susurro` thread pool with `Etiqueta.de("valenta/chunk")`
  for back-pressure.
- **Greedy face culling** — skips hidden faces against opaque neighbours, reducing
  triangle count by 60–80% in typical terrain.

### Visibility Culling
- **Frustum culling** — Gribb-Hartmann plane extraction, ~5 ns per section.
- **Occlusion queries** — hardware `GL_SAMPLES_PASSED` with one-frame-delayed
  readback; sections proven invisible skip rendering entirely.
- **PVS tree** — Potentially Visible Set pre-computation via portal flood-fill;
  sections behind walls are eliminated before the frustum test.

### Sky Optimisation
- `glInvalidateFramebuffer` when the camera is fully underground or indoors,
  saving bandwidth on tiled-rendering GPUs (integrated / mobile).

### Quality of Life (Sodium Extra)
| Feature | Config key | Values |
|---------|-----------|--------|
| Particle filtering | `quality.particles` | `none` / `reducir` / `ocultar` |
| Cloud rendering | `quality.clouds` | `vanilla` / `rapidas` / `desactivar` |
| Animated textures | `quality.animatedTextures` | `true` / `false` |
| Smooth render distance | `quality.minRenderDistanceSafe` | chunks (min 2) |
| GPU timing pane (F3) | `debug.showGpuTimings` | `true` / `false` |
| Occlusion debug overlay | `debug.showOcclusionOverlay` | `true` / `false` |

### Debug
- `/valenta debug occlusion` — toggles the culling visualisation overlay.
- `/valenta debug gpu` — toggles the GPU timing pane.
- `/valenta debug stats` — prints current-frame culling and timing statistics.

## Configuration

Edit `config/valenta.toml` (auto-generated on first launch). All settings are
hot-reloadable via `/valenta reload`.

## Compatibility

Valenta's Vifada morphs target `net/minecraft/client/renderer/...` classes and are
grouped under the namespace `valenta.escultor`. If another render mod transforms the
same methods, `VidaClassTransformer` reports the conflict at startup and allows the
user to choose priority.

See [docs/mods/valenta/compat-matrix.md](../../docs/mods/valenta/compat-matrix.md) for
the tested compatibility matrix.

## Building

```bash
./gradlew :mods:valenta:build
```

## Benchmarks

```bash
./gradlew :mods:valenta:jmhJar
java -jar mods/valenta/build/libs/valenta-jmh.jar
```

## License

Apache License 2.0 — see [LICENSE](../../LICENSE).
