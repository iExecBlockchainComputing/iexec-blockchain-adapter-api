node('docker') {

    stage("Git checkout"){
        checkout scm
    }

    stage('Test') {
        sh './gradlew build itest -i --no-daemon'
        junit 'build/test-results/**/*.xml'
    }

}

@Library('jenkins-library@master') _
buildSimpleDocker(imageprivacy: 'public')