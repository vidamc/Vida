# Telemetría v1 (opt-in)

- Activación explícita: `-Dvida.telemetry.enabled=true`. Por defecto está **desactivada**.
- Solo agregados numéricos en memoria: `freezeEvents`, `transformMorphNanosTotal`, y `coldStartNanos` **solo** si se registró un arranque. **Sin** PII, rutas absolutas ni red saliente en esta versión.
- Los snapshots (`TelemetriaV1.snapshotSinPii()`) están pensados para logs locales o CI controlado.
