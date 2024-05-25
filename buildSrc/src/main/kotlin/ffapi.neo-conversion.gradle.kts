import com.google.gson.JsonParser
import com.moandjiezana.toml.TomlWriter
import dev.architectury.at.AccessTransformSet
import dev.architectury.at.io.AccessTransformFormats
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.task.service.MappingsService
import net.fabricmc.loom.util.LfWriter
import net.fabricmc.loom.util.aw2at.Aw2At
import net.fabricmc.loom.util.service.BuildSharedServiceManager
import net.fabricmc.loom.util.service.UnsafeWorkQueueHelper
import kotlin.io.path.*

val versionMc: String by rootProject
val versionForge: String by rootProject

val loom = extensions.getByType<LoomGradleExtensionAPI>()

extensions.getByType<SourceSetContainer>().configureEach {
    // We have to capture the source set name for the lazy string literals,
    // otherwise it'll just be whatever the last source set is in the list.
    val baseTaskName = "ForgeModMetadata"
    val sourceSetName = name
    val resourceRoots = resources.srcDirs
    val taskName = getTaskName("generate", baseTaskName)
    val task = tasks.register(taskName, GenerateForgeModMetadata::class.java) {
        group = "sinytra"
        description = "Generates mods.toml files for $sourceSetName fabric mod."

        // Only apply to default source directory since we also add the generated
        // sources to the source set.
        sourceRoots.from(resourceRoots)
        outputDir = file("src/generated/$sourceSetName/resources")
        loaderVersionString = "1"
        forgeVersionString = versionForge
        minecraftVersionString = versionMc
        accessWidener = loom.accessWidenerPath
    }
    resources.srcDir(task)

    val cleanTask = tasks.register(getTaskName("clean", baseTaskName), Delete::class.java) {
        group = "sinytra"
        delete(file("src/generated/$sourceSetName/resources"))
    }
    tasks.named("clean") {
        dependsOn(cleanTask)
    }
    tasks.named("generate") {
        dependsOn(task)
    }
}

afterEvaluate { 
    if (loom.accessWidenerPath.isPresent) {
        tasks.withType<Jar> {
            exclude(loom.accessWidenerPath.get().asFile.name)
        }
    }
}

abstract class GenerateForgeModMetadata : DefaultTask() {
    @get:SkipWhenEmpty
    @get:InputFiles
    val sourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    val loaderVersionString: Property<String> = project.objects.property<String>()

    @get:Input
    val forgeVersionString: Property<String> = project.objects.property<String>()

    @get:Input
    val minecraftVersionString: Property<String> = project.objects.property<String>()

    @get:InputFile
    @get:Optional
    val accessWidener: RegularFileProperty = project.objects.fileProperty()

    private val mappingServiceUuid: Property<String> = project.objects.property<String>()

    @Inject
    protected abstract fun getBuildEventsListenerRegistry(): BuildEventsListenerRegistry

    init {
        val serviceManagerProvider = BuildSharedServiceManager.createForTask(this, getBuildEventsListenerRegistry())

        mappingServiceUuid.value(project.provider {
            UnsafeWorkQueueHelper.create(
                MappingsService.createDefault(
                    project,
                    serviceManagerProvider.get().get(),
                    MappingsNamespace.NAMED.toString(),
                    MappingsNamespace.MOJANG.toString()
                )
            )
        })
    }

    private fun normalizeModid(modid: String): String {
        return modid.replace('-', '_')
    }

    data class ModsToml(
        val modLoader: String,
        val loaderVersion: String,
        val license: String,
        val displayTest: String?,

        val mods: List<Mod>,
        val dependencies: Map<String, List<ModDependency>>,
        val mixins: List<Mixin>?,
        val properties: Map<String, String>?
    )

    data class ModDependency(
        val modId: String,
        val type: String,
        val versionRange: String,
        val ordering: String,
        val side: String
    )

    data class Mod(
        val modId: String,
        val version: String,
        val displayName: String,
        val logoFile: String?,
        val authors: String?,
        val description: String?,
        val provides: List<String>?
    )
    
    data class Mixin(
        val config: String
    )

    @TaskAction
    fun run() {
        val output = outputDir.get().asFile.toPath()
        project.delete(output)
        val containsCode = sourceRoots.any { File(it.parentFile, "java").exists() }
        for (sourceRoot in sourceRoots) {
            if (!sourceRoot.isDirectory()) {
                continue
            }

            val root = sourceRoot.toPath()
            val fabricMetadata = root.resolve("fabric.mod.json")

            if (fabricMetadata.notExists()) {
                continue
            }

            val json = fabricMetadata.bufferedReader().use(JsonParser::parseReader).asJsonObject

            val originalModid = json.get("id").asString
            val normalModid = normalizeModid(originalModid)
            val nextMajor = (minecraftVersionString.get().split('.')[1].toInt()) + 1
            val excludedDeps = listOf("fabricloader", "java", "minecraft")
            val modDependencies =
                (json.getAsJsonObject("depends")?.entrySet() ?: emptySet()).filter { !excludedDeps.contains(it.key) }.map {
                    val normalDepModid = normalizeModid(it.key as String)
                    return@map ModDependency(
                        normalDepModid,
                        "required",
                        "*",
                        "NONE",
                        "BOTH"
                    )
                }
            val allDependencies: List<ModDependency> = listOf(
                ModDependency(
                    "neoforge",
                    "required",
                    "[${forgeVersionString.get()},)",
                    "NONE",
                    "BOTH"
                ),
                ModDependency(
                    "minecraft",
                    "required",
                    "[${minecraftVersionString.get()},1.$nextMajor)",
                    "NONE",
                    "BOTH"
                )
            ) + modDependencies
            val displayTest = when(json.get("environment")?.asString) {
                "client" -> "IGNORE_ALL_VERSION"
                "server" -> "IGNORE_SERVER_VERSION"
                else -> null
            }
            val providedMods = buildList<String> {
                json.getAsJsonArray("provides")?.forEach { add(it.asString) }
                if (originalModid != normalModid) {
                    add(originalModid)
                }
            }
            val mods = listOf(
                Mod(
                    modId = normalModid,
                    version = "\${file.jarVersion}",
                    displayName = json.get("name").asString,
                    logoFile = json.get("icon")?.asString,
                    authors = json.getAsJsonArray("authors")?.map { it.asString }?.joinToString(separator = ", "),
                    description = json.get("description")?.asString,
                    provides = providedMods
                )
            )
            val mixins = json.getAsJsonArray("mixins")?.map { 
                if (it.isJsonObject) {
                    Mixin(it.asJsonObject.get("config").asString)
                } else if (it.isJsonPrimitive) {
                    Mixin(it.asString)
                } else {
                    throw RuntimeException("Unknown mixin config type $it")
                }
            }
            val properties =
                if (json.getAsJsonObject("entrypoints")?.has("fabric-gametest") == true) 
                    mapOf("forgified-fabric-api:game-test-prefix" to originalModid) 
                else 
                    null

            val modsToml = ModsToml(
                modLoader = if (containsCode) "javafml" else "lowcodefml",
                loaderVersion = "[${loaderVersionString.get()},)",
                license = json.get("license")?.asString ?: "All Rights Reserved",
                displayTest,

                mods,
                dependencies = mapOf(normalModid to allDependencies),
                mixins,
                properties
            )
            val modsTomlFile = output.resolve("META-INF/neoforge.mods.toml")
            modsTomlFile.deleteIfExists()
            modsTomlFile.parent.createDirectories()
            TomlWriter().write(modsToml, modsTomlFile.toFile())
        }

        if (accessWidener.isPresent) {
            val awPath = accessWidener.get().asFile.toPath()
            val atPath = output.resolve("META-INF/accesstransformer.cfg")
        
            var at = AccessTransformSet.create()
            awPath.bufferedReader().use { at.merge(Aw2At.toAccessTransformSet(it)) }
        
            val service = UnsafeWorkQueueHelper.get(mappingServiceUuid, MappingsService::class.java)
        
            at = at.remap(service.memoryMappingTree, service.fromNamespace, service.toNamespace)
        
            LfWriter(atPath.bufferedWriter()).use {  AccessTransformFormats.FML.write(it, at) }
        }
    }
}