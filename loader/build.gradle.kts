/*
 * :loader — рантайм Vida: Java Agent, ClassFileTransformer, загрузчики
 * классов (JuegoLoader, ModLoader), оркестрация бутстрапа.
 *
 * Публикуется как обычный jar-артефакт И одновременно является валидным
 * Java-агентом: в его манифесте прописаны Premain-Class/Agent-Class и
 * нужные Can-* атрибуты.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida runtime: Java Agent, class transformers, classloader hierarchy."

// mc-stubs — source-only стабы MC-классов, которые использует dev.vida.platform.*
// Они НЕ попадают в публикуемый jar (отдельный sourceSet, не main). На рантайме
// реальные классы предоставляет Minecraft; compileOnly нужен только для javac.
val mcStubs by sourceSets.creating {
    java.srcDir("mc-stubs")
}

dependencies {
    api(project(":core"))
    api(project(":manifest"))
    api(project(":discovery"))
    api(project(":resolver"))
    api(project(":vifada"))
    api(project(":escultores"))
    api(project(":fuente"))
    api(project(":mundo"))
    api(project(":base"))
    api(project(":render"))
    api(project(":cartografia"))

    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.slf4j.api)

    // MC-стабы: только compile-time. Ничего не тащится в runtime-классpath,
    // и при сборке agentJar стабы явно выключаются из fat-jar (см. ниже).
    compileOnly(mcStubs.output)

    testRuntimeOnly(libs.logback.classic)
}

// Contract tests read the same list as the shipped META-INF resource.
tasks.named<org.gradle.api.tasks.Copy>("processTestResources") {
    val supportedList = rootProject.layout.projectDirectory.file("platform-profiles/supported-contract-profiles.txt")
    inputs.file(supportedList).withPropertyName("supportedContractProfiles")
    from(supportedList) {
        into(".")
        rename { "supported-contract-profiles.txt" }
    }
}

// ------------------------------------------------------------------------
//  Version-stamping: подставляем актуальную project.version в properties
//  файл загрузчика, чтобы синтетический провайдер "vida" знал свою версию.
// ------------------------------------------------------------------------
tasks.named<ProcessResources>("processResources") {
    val vidaVersion = project.version.toString()
    inputs.property("vidaVersion", vidaVersion)
    val platformProfiles = rootProject.layout.projectDirectory.dir("platform-profiles/generations")
    inputs.dir(platformProfiles).withPropertyName("platformProfiles")
    from(platformProfiles) {
        into("META-INF/vida/platform-profiles")
    }
    val supportedList = rootProject.layout.projectDirectory.file("platform-profiles/supported-contract-profiles.txt")
    inputs.file(supportedList).withPropertyName("supportedContractProfiles")
    from(supportedList) {
        into("META-INF/vida")
        rename { "supported-contract-profiles.txt" }
    }
    filesMatching("META-INF/vida/loader-version.properties") {
        expand(mapOf("vidaVersion" to vidaVersion))
    }
}

// Общие атрибуты агента — переиспользуются обычным jar'ом и агентным fat-jar'ом.
//
// Can-Redefine-Classes / Can-Retransform-Classes намеренно false:
//   * Мы применяем морфы и Escultor'ы в define-time (внутри ClassFileTransformer.
//     transform), до первого использования класса. Этого достаточно — Minecraft
//     ещё не успел загрузить game-classes к моменту premain.
//   * Если объявить Can-Retransform-Classes=true, JVM обязана хранить оригинальный
//     байткод ВСЕХ загруженных классов на случай retransform'а; это удваивает
//     metaspace и включает более медленный class-define path. На hot-path
//     (чанки, сущности, пакеты) это даёт микро-фризы. Для заявленной
//     «безумной» производительности — неприемлемо.
//   * Если в будущем мод захочет runtime-ретрансформацию, включим её через
//     опциональный ко-агент, а не по умолчанию для всех.
val agentManifestAttrs = mapOf(
    "Premain-Class"                to "dev.vida.loader.VidaPremain",
    "Agent-Class"                  to "dev.vida.loader.VidaPremain",
    "Can-Redefine-Classes"         to "false",
    "Can-Retransform-Classes"      to "false",
    "Can-Set-Native-Method-Prefix" to "false",
    "Implementation-Title"         to "Vida Loader",
    "Implementation-Vendor"        to "The Vida Project",
)

tasks.named<Jar>("jar") {
    manifest { attributes(agentManifestAttrs) }
}

// -------------------------------------------------------------------------
//  agentJar — самодостаточный jar, годный для -javaagent:.
//
//  Обычный :loader:jar содержит только классы :loader и не может запускаться
//  как Java-агент в вакууме: JVM найдёт Premain-Class, но при первом же
//  обращении к dev.vida.core.* / ASM / SLF4J упадёт с NoClassDefFoundError,
//  и процесс завершится ДО любых логов игры. Именно этот jar мы кладём в
//  libraries/ клиентского .minecraft, поэтому тут ОБЯЗАН быть fat-jar.
//
//  Задача аккуратно выкидывает META-INF подписей и MANIFEST'ы зависимостей,
//  иначе JVM не примет результирующий манифест или будет ругаться на
//  нарушенные подписи.
// -------------------------------------------------------------------------
val agentJar by tasks.registering(Jar::class) {
    description = "Self-contained -javaagent jar: :loader + all runtime deps."
    group = "build"
    archiveClassifier.set("agent")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(agentManifestAttrs) }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    inputs.files(configurations.runtimeClasspath).withPropertyName("runtimeClasspath")
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/*.EC",
        "META-INF/MANIFEST.MF",
        "META-INF/LICENSE*",
        "META-INF/NOTICE*",
        "META-INF/versions/*/module-info.class",
        "module-info.class",
        // MC-стабы: нужны только для javac при сборке :loader. На рантайме
        // реальные классы предоставляет Minecraft, и наличие стабов в
        // classpath агента ломает запуск (дубликат net.minecraft.* классов).
        "net/minecraft/**",
    )
}

tasks.named("assemble").configure { dependsOn(agentJar) }
