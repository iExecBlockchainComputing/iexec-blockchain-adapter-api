node('docker') {

    stage("Git checkout"){
        checkout scm
    }

    stage('Cleaning') {
        try {
            sh "docker rm -f chain broker blockchain-adapter-mongo & " +
                    "docker network create iexec-blockchain-net"
        } catch (err) {
            echo err.getMessage()
        }
    }

    stage('Setup') {
        withCredentials([string(credentialsId: 'BROKER_PRIVATE_KEY', variable: 'brokerPrivateKey')]) {
            sh "BROKER_PRIVATE_KEY=${brokerPrivateKey} docker-compose up -d"
        }
    }

    stage('Test') {
        sh './gradlew build itest -i --no-daemon'
        junit 'build/test-results/**/*.xml'
    }

    stage('Teardown') {
        sh 'docker-compose down'
    }

}

@Library('jenkins-library@master') _
buildSimpleDocker(imageprivacy: 'public')