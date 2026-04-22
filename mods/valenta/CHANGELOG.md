# Valenta Changelog

## [1.0.0] — 2026-04-22

Initial release as part of Vida 0.9.0.

### Added

- **Render core**: VBO batcher with `glMultiDrawElementsIndirect`, compact 16-byte
  vertex format, biome-blending SSBO, block-light SSBO.
- **Chunk meshing**: three-stage task-graph (`Analisis → Build → Upload`) via
  `Susurro` with `Etiqueta.de("valenta/chunk")` back-pressure.
- **Culling engine**: Gribb-Hartmann frustum, hardware occlusion queries with
  one-frame-delayed readback, PVS tree with portal flood-fill.
- **Sky optimisation**: `glInvalidateFramebuffer` when camera is fully enclosed.
- **Quality of life**: particle filter (`none`/`reducir`/`ocultar`), cloud renderer
  (`vanilla`/`rapidas`/`desactivar`), animated texture toggle, smooth render distance
  transitions, GPU timing debug pane.
- **Vifada morphs**: `LevelRendererMorph`, `GameRendererMorph`,
  `ChunkRendererMorph`, `ParticleMorph`, `CloudMorph` — all with
  `requireTarget = false` for graceful degradation.
- **Debug commands**: `/valenta debug occlusion`, `/valenta debug gpu`,
  `/valenta debug stats`.
- **Tests**: unit tests for all core components, jqwik property-based tests for
  meshing invariants.
- **JMH benchmarks**: `VertexFormatBenchmark`, `CullingBenchmark`,
  `MeshingBenchmark`.
- **Documentation**: README, architecture guide, compatibility matrix.
