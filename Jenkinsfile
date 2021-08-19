node('docker') {

    stage("Git checkout"){
        checkout scm
    }

    stage('Setup integration') {
        withCredentials([string(credentialsId: 'BROKER_PRIVATE_KEY', variable: 'brokerPrivateKey')]) {
            sh "docker rm -f chain broker blockchain-adapter-mongo & " +
                    "docker network create iexec-blockchain-net & " +
                    "BROKER_PRIVATE_KEY=${brokerPrivateKey} docker-compose up -d"
        }
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