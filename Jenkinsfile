@Library('jenkins-library@1.3.0') _
buildJavaProject(
        integrationTestsEnvVars: ["BROKER_PRIVATE_KEY"],
        shouldPublishJars: false,
        shouldPublishDockerImages: true,
        dockerfileDir: './docker',
        //dockerfileFilename: "Dockerfile-local",
        buildContext: '.',
        //dockerImageRepositoryName: '',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
