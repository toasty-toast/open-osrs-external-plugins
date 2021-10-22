import ProjectVersions.openosrsVersion

version = "1.0.1"

project.extra["PluginName"] = "Group Iron Panel"
project.extra["PluginDescription"] = "Adds a panel showing your group members' stats"

dependencies {
    annotationProcessor(Libraries.lombok)
    annotationProcessor(Libraries.pf4j)

    implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.9.1")

    compileOnly("com.openosrs:http-api:$openosrsVersion+")
    compileOnly("com.openosrs:runelite-api:$openosrsVersion+")
    compileOnly("com.openosrs:runelite-client:$openosrsVersion+")

    compileOnly(Libraries.guice)
    compileOnly(Libraries.javax)
    compileOnly(Libraries.lombok)
    compileOnly(Libraries.pf4j)
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                    "Plugin-Version" to project.version,
                    "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                    "Plugin-Provider" to project.extra["PluginProvider"],
                    "Plugin-Description" to project.extra["PluginDescription"],
                    "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}