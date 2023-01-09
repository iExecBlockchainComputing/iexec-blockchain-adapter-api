@Library('global-jenkins-library@2.2.3') _
buildJavaProject(
        buildInfo: getBuildInfo(),
        integrationTestsEnvVars: ['BROKER_PRIVATE_KEY'],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: 'docker',
        buildContext: '.',
        preDevelopVisibility: 'iex.ec',
        developVisibility: 'iex.ec',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
