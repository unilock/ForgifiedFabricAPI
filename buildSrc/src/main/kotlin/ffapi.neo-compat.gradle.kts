import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.task.RemapJarTask
import java.io.FileOutputStream

val versionMc: String by rootProject
val upstreamVersion: String by rootProject.ext
val versionFabricLoader: String by rootProject
val versionJCC: String by rootProject

val loom = extensions.getByType<LoomGradleExtensionAPI>() as LoomGradleExtension

val apiLibs: Configuration by configurations.creating {
    extendsFrom(configurations.getByName("api"))
    isCanBeResolved = true
}
val baseLib: Configuration by configurations.creating
val referenceApi: Configuration by configurations.creating
val jarCompatChecker: Configuration by configurations.creating

dependencies {
    baseLib("net.fabricmc:fabric-loader:$versionFabricLoader")

    referenceApi("net.fabricmc.fabric-api:fabric-api:$upstreamVersion+$versionMc")
    referenceApi("net.fabricmc.fabric-api:fabric-api-deprecated:$upstreamVersion+$versionMc")

    jarCompatChecker("net.neoforged:jarcompatibilitychecker:$versionJCC:all")
}

val compareJar by tasks.registering(Jar::class) {
    from(zipTree(tasks.named<Jar>("jar").flatMap { it.archiveFile }))
    archiveClassifier = "compare"
    destinationDirectory = project.layout.buildDirectory.dir("devlibs")
}

val remapReferenceApi by tasks.creating(RemapJarTask::class) {
    group = "sinytra"
    outputs.cacheIf { true }
    inputFile.fileProvider(provider {
        val deps = referenceApi.resolvedConfiguration
        val referenceDep =
            deps.lenientConfiguration.allModuleDependencies.find { it.moduleGroup == "net.fabricmc.fabric-api" && it.moduleName == project.name }
        return@provider referenceDep!!.allModuleArtifacts.first().file
    })
    sourceNamespace = MappingsNamespace.INTERMEDIARY.toString()
    targetNamespace = MappingsNamespace.NAMED.toString()
    destinationDirectory = project.layout.buildDirectory.dir(name)
    classpath.from(loom.getMinecraftJarsCollection(MappingsNamespace.INTERMEDIARY))
}

val checkReferenceCompatibility by tasks.registering(JavaExec::class) {
    group = "verification"

    classpath(jarCompatChecker)
    mainClass = "net.neoforged.jarcompatibilitychecker.ConsoleTool"
    args("--api", "--annotation-check-mode", "warn_added", "--internal-ann-mode", "skip")
    val outputLog = project.layout.buildDirectory.dir(name).map { it.file("output.log") }
    inputs.file(remapReferenceApi.inputFile)
        .withPropertyName("inputFile")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(outputLog)
        .withPropertyName("outputFile")
    outputs.cacheIf { true }
    doFirst {
        standardOutput = FileOutputStream(outputLog.get().asFile)
    }
}

afterEvaluate {
    tasks.configureEach {
        if (name == "prepareRemapReferenceApi") {
            doFirst {
                loom.mixin.useLegacyMixinAp = false
            }
            doLast {
                loom.mixin.useLegacyMixinAp = true
            }
        }
    }
    checkReferenceCompatibility.configure {
        dependsOn(compareJar, remapReferenceApi)
        args(
            "--base-jar",
            remapReferenceApi.archiveFile.get().asFile.absolutePath,
            "--input-jar",
            compareJar.get().archiveFile.get().asFile.absolutePath
        )
        (configurations.getByName("minecraftNamedCompile") + configurations.getByName("apiLibs")).forEach {
            args(
                "--lib",
                it.absolutePath
            )
        }
        baseLib.resolve().forEach { args("--base-lib", it.absolutePath) }
    }
}

tasks.named("check") {
    dependsOn(checkReferenceCompatibility)
}