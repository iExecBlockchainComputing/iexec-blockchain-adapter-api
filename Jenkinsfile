@Library('jenkins-library@feature/build-java-project') _
buildJavaProject(
        dockerfileDir: './docker',
        buildContext: '.',
        //dockerImageRepositoryName: '',
        preReleaseVisibility: 'docker.io',
        releaseVisibility: 'docker.io',
        runIntegration: true,
        itEnvVars: ["BROKER_PRIVATE_KEY"],
        publishJars: true
)
