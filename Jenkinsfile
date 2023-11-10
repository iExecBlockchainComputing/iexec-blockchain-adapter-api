@Library('global-jenkins-library@2.7.3') _
buildJavaProject(
        buildInfo: getBuildInfo(),
        integrationTestsEnvVars: ['BROKER_PRIVATE_KEY'],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: '.',
        buildContext: '.')
