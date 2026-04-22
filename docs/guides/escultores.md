# Escultores — низкоуровневые трансформеры

Когда `@VifadaMorph` недостаточно — например, нужно модифицировать класс без статического описания в morph-классе, или работать с константным пулом напрямую — в дело идут `Escultor`'ы.

Это внутренний API Vida, и пока что не рекомендован для мод-авторов: он преимущественно `@ApiStatus.Internal`. Страница — для мод-инженеров и контрибьюторов.

## Концепция

`Escultor` — это функция `byte[] → byte[]`:

```java
public interface Escultor {
    String name();
    boolean mightMatch(String className, byte[] classFile);
    byte[] tryPatch(String className, byte[] classFile);
}
```

- `name()` — диагностическое имя.
- `mightMatch(...)` — **дешёвый** pre-check. Если `false`, `tryPatch` не вызывается.
- `tryPatch(...)` — настоящая работа. Возвращает `null`, если решил не менять.

Ключевой принцип: **`mightMatch` должен быть O(1) или O(размер класса)** — он вызывается на _каждом_ классе игры. `tryPatch` — O(что нужно), но только после положительного `mightMatch`.

## Пример: `BrandingEscultor`

Патчит format-строку F3-дебаг-экрана.

### Pre-check — byte-scan

```java
private static final byte[] NEEDLE = "Minecraft %s (%s/%s%s)".getBytes(UTF_8);

@Override
public boolean mightMatch(String name, byte[] bytes) {
    // ищем ASCII-подстроку в сыром .class файле
    return indexOf(bytes, NEEDLE) >= 0;
}
```

`indexOf` — наивный O(n·m), но `m` маленькое и `n` — размер одного класса. На 99% классов `mightMatch` отвечает `false` за считанные микросекунды.

### Patch — ASM

```java
@Override
public byte[] tryPatch(String name, byte[] bytes) {
    ClassReader cr = new ClassReader(bytes);
    ClassNode cn = new ClassNode();
    cr.accept(cn, 0);  // 0, не SKIP_FRAMES — иначе невалидный byte-code на выходе

    boolean changed = false;
    for (MethodNode mn : cn.methods) {
        for (AbstractInsnNode insn : mn.instructions) {
            if (insn instanceof LdcInsnNode ldc && "Minecraft %s (%s/%s%s)".equals(ldc.cst)) {
                ldc.cst = "Minecraft %1$s (vida)";
                changed = true;
            }
        }
    }

    if (!changed) return null;

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
}
```

### Регистрация

Эскульторы встраиваются в `VidaClassTransformer` после `MorphIndex`:

```java
transformer.registerEscultor(new BrandingEscultor());
```

В 0.x API регистрации ещё не публичный. Эскультор-хуки в `vida.mod.json` — запланированы.

## Когда писать свой `Escultor`

- Вам нужно модифицировать класс, имя которого вычисляется (например, переименованный при обфускации).
- Нужна патчинг-логика, которая сама решает применяться ли (`Vifada` требует статический `@VifadaMorph`).
- Нужна работа с constant pool, bootstrap-методами, атрибутами класса.

Для 99% мод-задач `Vifada` достаточно. `Escultor` — это «спуститься ниже», и делать это стоит осознанно.

## Правила хорошего тона

### 1. Делайте `mightMatch` _очень_ быстрым

Любой `byte[]`-scan не быстрее, чем `indexOf` по константной needle. Если вы делаете ASM-парсинг в `mightMatch` — вы делаете что-то не так.

### 2. Никогда не бросайте из `tryPatch` исключения

Возвращайте `null` при любом неожиданном состоянии. Исключения транзитивно приведут к провалу загрузки класса игры, что игрок не сможет диагностировать.

### 3. Логируйте применения

```java
log.info("BrandingEscultor: patched {}", name);
```

Это бесценно в issue-репортах от пользователей.

### 4. Тестируйте на loadability

Минимальный unit-тест:

```java
@Test
void patchedClassLoadsAndVerifies() {
    byte[] original = Files.readAllBytes(Path.of("src/test/resources/DebugScreenOverlay.class"));
    byte[] patched = new BrandingEscultor().tryPatch("net/minecraft/.../DebugScreenOverlay", original);

    // загружаем в in-memory ClassLoader
    TestClassLoader loader = new TestClassLoader();
    loader.define("net.minecraft....DebugScreenOverlay", patched);
    // никаких исключений — значит bytecode валидный
}
```

## Будущее

- Публичный API для мод-авторов (preview-статус).
- `@Escultor(priority = ...)` — декларативная регистрация.
- Кэш результатов `tryPatch` по хэшу входа — чтобы не патчить повторно одни и те же классы между запусками.

## Что читать дальше

- [modules/loader.md](../modules/loader.md#brandingescultor) — пример в коде.
- [architecture/performance.md](../architecture/performance.md) — почему pre-check важен.
- [guides/vifada.md](./vifada.md) — высокоуровневая альтернатива.
