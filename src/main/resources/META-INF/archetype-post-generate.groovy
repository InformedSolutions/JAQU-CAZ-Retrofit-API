import java.nio.file.Paths

def projectPath = Paths.get(request.outputDirectory, request.artifactId)

["mvnw", "mvnw.cmd"].each {
    def mavenWrapperFile = projectPath.resolve(it).toFile()
    mavenWrapperFile.setExecutable(true, false)
}