rootProject.name = "external-plugins"

include(":always-make-all")
include(":bank-pin")
include(":group-iron-panel")
include(":herbologist")

for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}