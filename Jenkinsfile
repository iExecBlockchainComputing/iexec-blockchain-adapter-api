node('docker') {

    stage("Git checkout"){
        checkout scm
    }

    stage('Setup integration') {
        sh 'docker network create iexec-blockchain-net & docker-compose up -d'
    }

    stage('Test') {
        sh './gradlew build itest -i --no-daemon'
        junit 'build/test-results/**/*.xml'
    }

    stage('Shutdown integration') {
        sh 'docker-compose down'
    }

}

@Library('jenkins-library@master') _
buildSimpleDocker(imageprivacy: 'public')