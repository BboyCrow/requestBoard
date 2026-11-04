plugins {
    // Applies the right Loom variant for the Minecraft version being built.
    id("dev.kikugie.loom-back-compat")
}

val modVersion = property("mod.version") as String
version = "$modVersion+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

// Fabric's client gametest API does not exist on 1.21.1.
val clientGameTests = sc.current.parsed >= "1.21.11"

loom {
    splitEnvironmentSourceSets()

    mods {
        create("requestboard") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runConfigs.all {
        runDirectory = rootProject.file("run/${sc.current.version}")
    }
}

if (clientGameTests) {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "requestboard-gametest"
            enableGameTests = false
            eula = true
        }
    }
}

// JSON data whose format changed between versions (recipes, loot tables, item models, advancements),
// written by tools/generate_assets.py.
sourceSets["main"].resources.srcDir(rootProject.file("generated/${sc.current.version}"))

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang mappings on obfuscated versions, so every version uses the same names.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to sc.properties.get<String>("deps.fabric_loader"),
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    withType<Jar> {
        from(rootProject.file("LICENSE")) { rename { "${it}_requestboard" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/<mod version>/"
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}
