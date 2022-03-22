@Library('global-jenkins-library@feature/external-iexec-oci') _
buildJavaProject(
        buildInfo: getBuildInfo(),
        integrationTestsEnvVars: ['BROKER_PRIVATE_KEY'],
        shouldPublishJars: false,
        shouldPublishDockerImages: true,
        dockerfileDir: 'docker',
        buildContext: '.',
        preDevelopVisibility: 'iex.ec',
        developVisibility: 'iex.ec',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
