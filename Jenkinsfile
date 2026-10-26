pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'develop', description: 'Branche à construire')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyse SonarQube')
        booleanParam(name: 'PUBLISH_NEXUS', defaultValue: true, description: 'Publier le JAR sur Nexus')
        string(name: 'SONAR_HOST_URL', defaultValue: 'http://host.docker.internal:9000', description: 'URL SonarQube')
        string(name: 'NEXUS_HOST', defaultValue: 'host.docker.internal:8081', description: 'Hôte Nexus, sans http://')
    }

    stages {
        stage('CHECKOUT GIT') {
            steps {
                git branch: params.BRANCH, url: 'https://github.com/Habib-Amami/Projet-DevOps.git'
            }
        }
        stage('MVN CLEAN') {
            steps {
                dir('backend') {
                    sh 'sh ./mvnw -B clean'
                }
            }
        }
        stage('ARTIFACT CONSTRUCTION') {
            steps {
                dir('backend') {
                    sh 'sh ./mvnw -B package -DskipTests'
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
                    sh 'sh ./mvnw -B test'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }
        stage('MVN SONARQUBE') {
            when { expression { params.RUN_SONAR } }
            steps {
                dir('backend') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            sh ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
                              -Dsonar.projectKey=achat \
                              -Dsonar.projectName=achat \
                              -Dsonar.host.url="$SONAR_HOST_URL" \
                              -Dsonar.token="$SONAR_TOKEN" \
                              -Dsonar.ws.timeout=300 \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }
        stage('PUBLISH TO NEXUS') {
            when { expression { params.PUBLISH_NEXUS } }
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: params.NEXUS_HOST,
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