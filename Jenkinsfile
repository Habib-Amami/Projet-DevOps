pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('CHECKOUT GIT') {
            steps {
                git branch: 'develop', url: 'https://github.com/Habib-Amami/Projet-DevOps.git'
            }
        }
        stage('MVN CLEAN') {
            steps {
                dir('backend') {
                    sh './mvnw clean'
                }
            }
        }
        stage('ARTIFACT CONSTRUCTION') {
            steps {
                dir('backend') {
                    sh './mvnw package -DskipTests'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }
        stage('UNIT TESTS') {
            steps {
                dir('backend') {
                    sh './mvnw test'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }
        stage('MVN SONARQUBE') {
            steps {
                dir('backend') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            ./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
                              -Dsonar.projectKey=achat \
                              -Dsonar.projectName=achat \
                              -Dsonar.host.url=http://host.docker.internal:9000 \
                              -Dsonar.token=$SONAR_TOKEN \
                              -Dsonar.ws.timeout=300 \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }
        stage('PUBLISH TO NEXUS') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: 'host.docker.internal:8081',
                    groupId: 'org.example',
                    version: '0.0.1-SNAPSHOT',
                    repository: 'maven-snapshots',
                    credentialsId: 'nexus-credentials',
                    artifacts: [
                        [artifactId: 'achat', classifier: '', file: 'backend/target/achat-0.0.1-SNAPSHOT.jar', type: 'jar']
                    ]
                )
            }
        }
    }
}