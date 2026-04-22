# Valenta — Architecture

> Internal design document for the Valenta rendering optimisation mod.

## Overview

Valenta is a Sodium-class mod that replaces Minecraft's chunk rendering
pipeline with a modern OpenGL 4.3+ backend. It is structured as a Vida mod
(`mods/valenta/`) with its own lifecycle, configuration, and Vifada morphs.

## Module Map

```
mods/valenta/
├── core/         ← GPU abstractions: buffers, vertex format, batcher
├── chunk/        ← Meshing pipeline: analysis → build → upload
├── culling/      ← Visibility: frustum, occlusion queries, PVS tree
├── sky/          ← Sky optimisation: framebuffer invalidation
├── quality/      ← QoL: particles, clouds, textures, render distance
├── escultor/     ← Vifada morphs injecting into MC renderer classes
├── debug/        ← Debug overlay and /valenta commands
├── ValentaMod    ← Entrypoint: wires all subsystems together
└── ValentaConfig ← Configuration from valenta.toml
```

## Render Pipeline

### Per-Frame Flow

```
1. AnalisisEtapa   (render thread, <100µs)
   └─ scans dirty sections, sorts by camera distance
   └─ produces List<SectionRequest>

2. ChunkTaskGraph.dispatch()   (render thread → Susurro workers)
   └─ for each SectionRequest, launches BuildEtapa on a worker
   └─ tagged Etiqueta.de("valenta/chunk") for back-pressure

3. BuildEtapa.construir()   (Susurro worker thread)
   └─ greedy meshing: cull hidden faces, emit quads
   └─ produces MallaChunk (immutable, thread-safe)

4. ChunkTaskGraph.collect()   (render thread)
   └─ drains ConcurrentLinkedQueue of completed meshes

5. UploadEtapa.subir()   (render thread)
   └─ copies vertex/index data into mega-VBO via VboMallaBatcher
   └─ copies biome/light data into SSBOs

6. VboMallaBatcher.flush()   (render thread)
   └─ uploads IndirectDrawBuffer → GL_DRAW_INDIRECT_BUFFER
   └─ glMultiDrawElementsIndirect(GL_TRIANGLES, ...)

7. CullingEngine.testSection()   (render thread, per section)
   └─ PVS → Frustum → OcclusionQuery three-tier gate
```

### Vertex Format

Standard vanilla uses 28 bytes per vertex:
```
position (12) + color (4) + texcoord (8) + light (4) = 28 bytes
```

Valenta's `CompactVertexFormat` uses 16 bytes:
```
posXYZ (6, short3) + normal (2, byte2) + color (4, RGBA8) + texUV (4, short2) = 16 bytes
```

Positions are quantised to 1/4096 of a block (sub-pixel accuracy at any zoom).
Normals store X and Y; Z is reconstructed in the vertex shader via
`nz = sqrt(1 - nx² - ny²)`.

### Multi-Draw Indirect

Instead of one `glDrawElements` per section, Valenta:

1. Copies all section vertices into a single VBO.
2. Builds one `DrawElementsIndirectCommand` (20 bytes) per section.
3. Issues `glMultiDrawElementsIndirect` once per render pass.

This reduces CPU-side GL driver overhead from O(sections) to O(1).

## Culling

### Tier 1 — PVS (compile-time)

Pre-computed via portal flood-fill. Each section stores a compressed set
of sections it can see through transparent faces. Eliminates ~30–60% of
sections in enclosed environments.

### Tier 2 — Frustum (per-frame)

Gribb-Hartmann plane extraction from the MVP matrix. ~5 ns per AABB test.

### Tier 3 — Occlusion Queries (per-frame, GPU-side)

`GL_SAMPLES_PASSED` with one-frame-delayed readback. Sections with zero
samples are skipped on the next frame. Conditional rendering
(`glBeginConditionalRender`) is available for borderline sections.

## Threading Model

```
Render Thread ─────────────────────────────────────────────
  │  analisis  │  collect  │  upload  │  flush  │  culling
  │            │           │          │         │
Worker Pool (Susurro) ─────────────────────────────────────
  │── build(section A) ──────│
  │── build(section B) ────────│
  │── build(section C) ──│
```

All GPU calls happen on the render thread. Worker threads only produce
immutable `MallaChunk` objects that are consumed via a lock-free queue.

## Configuration

`valenta.toml` is read via `AjustesTipados` at mod startup. All settings
are documented in the TOML file itself. The `meshWorkers` setting defaults
to `max(2, availableProcessors/2)`, matching `Susurro`'s default policy.

## Compatibility

Morphs target:
- `LevelRenderer.setupRender`, `LevelRenderer.renderLevel`, `LevelRenderer.renderClouds`
- `GameRenderer.renderLevel`
- `SectionRenderDispatcher.runTask`
- `ParticleEngine.add`

All morphs use `requireTarget = false` for graceful degradation on
unsupported MC versions. Priority 800–900 ensures Valenta's hooks run
before most user mods.
