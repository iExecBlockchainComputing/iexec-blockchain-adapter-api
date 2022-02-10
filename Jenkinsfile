@Library('global-jenkins-library@1.3.0') _
buildJavaProject(
        integrationTestsEnvVars: ["BROKER_PRIVATE_KEY"],
        shouldPublishJars: false,
        shouldPublishDockerImages: true,
        dockerfileDir: './docker',
        buildContext: '.',
        //dockerImageRepositoryName: '',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
