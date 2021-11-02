@Library('jenkins-library@feature/build-java-project') _
buildJavaProject(
        shouldRunIntegrationTests: true,
        integrationTestsEnvVars: ["BROKER_PRIVATE_KEY"],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: './docker',
        //dockerfileFilename: "Dockerfile-local",
        buildContext: '.',
        //dockerImageRepositoryName: '',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
