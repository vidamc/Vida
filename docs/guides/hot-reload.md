# Hot reload (desarrollo)

Solo para desarrollo — **no** usar en producción.

## Gradle

En el proyecto del mod:

```kotlin
vida.run {
    hotReload.set(true)
}
```

La tarea `vidaRun` pasa `-Dvida.dev.hotReload=true` y `-Dvida.dev.hotReload.watch=<build/classes/java/main>`.

## Comportamiento

Con estas propiedades, tras el arranque el loader puede iniciar un hilo daemon que observa el directorio de clases compiladas. Cuando cambian archivos (`.class` u otros), se llama a `CatalogoManejador.reiniciarParaHotReloadDesarrollo()`, vaciando los catálogos para que el mod vuelva a registrar contenido.

## Limitaciones

- No redefine clases ya cargadas en la JVM de vanilla salvo herramientas externas (DCEVM, etc.).
- Los morfos Vifada aplicados a clases del juego **no** se recalculan tras el primer `defineClass`.
- Tras un reset de catálogos, los entrypoints del mod deben volver a ejecutar el registro; si el mod solo registra en `iniciar`, puede hacer falta reiniciar el cliente para un estado coherente.

Para datos JSON datapack (`vida:dataDriven`), tras recompilar recursos conviene copiar el JAR al directorio de mods o usar el flujo de recarga de recursos del juego.
