pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Backend tests and package') {
            steps {
                dir('backend') {
                    sh 'sh mvnw -B clean verify'
                }
            }
        }

        stage('Frontend tests and build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm test -- --watch=false --no-progress'
                    sh 'npm run build'
                }
            }
        }
    }

    post {
        always {
            junit(
                testResults: 'backend/target/surefire-reports/*.xml',
                allowEmptyResults: true
            )
        }
    }
}
