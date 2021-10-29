@Library('jenkins-library@feature/build-java-project') _
//buildSimpleDocker(imageprivacy: 'public')
buildJavaProject(
        dockerfileDir: './docker',
        buildContext: '.',
        //dockerImageRepositoryName: '',
        preReleaseVisibility: 'docker.io',
        releaseVisibility: 'docker.io',
        runIntegration: true,
        publishJars: false
)
