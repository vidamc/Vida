# Valenta — Compatibility Matrix

> Tracking compatibility with other rendering-related mods.

## Tested Configurations

| Mod | Version | Status | Notes |
|-----|---------|--------|-------|
| Vida base | 0.9.0+ | Compatible | Required dependency |
| Saciedad | 1.0.0 | Compatible | HUD overlay unaffected by render pipeline changes |
| Senda | 1.0.0 | Compatible | Uses Latidos/Catalogo only, no render hooks |

## Known Conflicts

| Mod | Conflict Area | Resolution |
|-----|--------------|------------|
| Other render optimisers | `LevelRenderer.renderLevel` morph | `VidaClassTransformer` reports conflict; user sets priority in `vida.mod.json` |
| Shader mods | `GameRenderer.renderLevel` | Valenta yields to shader mods via lower priority on render pass hooks |

## GPU Driver Notes

Valenta targets **OpenGL 4.3 core profile** (for `glMultiDrawElementsIndirect`
and SSBOs). Known driver-specific issues:

| GPU | Driver | Issue | Workaround |
|-----|--------|-------|------------|
| Intel HD 4000 | Mesa | No GL 4.3 support | Valenta falls back to per-section draws |
| AMD GCN 1.0 | AMDGPU | Slow SSBO writes | Disable `biomeBlendingSsbo` in config |
| NVIDIA Kepler | 470.xx | No issues | — |
| Apple M1 (MoltenVK) | N/A | Metal translation layer | Test pending |

## Reporting Issues

If you encounter a compatibility issue:

1. Check `logs/latest.log` for `VidaClassTransformer` conflict messages.
2. Set `debug.showGpuTimings = true` in `valenta.toml` to identify the slow pass.
3. File an issue with the log and your GPU driver version.
