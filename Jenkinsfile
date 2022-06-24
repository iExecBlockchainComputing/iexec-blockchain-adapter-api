@Library('global-jenkins-library@feature/sonar-pro-migration') _
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
