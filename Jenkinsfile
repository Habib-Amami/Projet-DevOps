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
        string(name: 'SONAR_HOST_URL', defaultValue: 'http://sonarqube:9000', description: 'URL SonarQube')
        string(name: 'NEXUS_HOST', defaultValue: 'nexus:8081', description: 'Hôte Nexus, sans http://')
        booleanParam(name: 'DOCKER_PUSH', defaultValue: true, description: 'Construire et pousser les images sur Docker Hub')
        string(name: 'DOCKERHUB_USER', defaultValue: '', description: 'Utilisateur Docker Hub (préfixe des images)')
        booleanParam(name: 'DEPLOY_K8S', defaultValue: true, description: 'Déployer sur le cluster EKS (créé par terraform/)')
        string(name: 'EKS_CLUSTER', defaultValue: 'achat-eks', description: 'Nom du cluster EKS')
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'Région AWS du cluster')
        choice(name: 'TERRAFORM_ACTION', choices: ['none', 'plan', 'apply', 'destroy'], description: 'Action Terraform (none = ne rien faire)')
        string(name: 'EMAIL_TO', defaultValue: '', description: 'Destinataires email séparés par des virgules (vide = désactivé)')
    }

    environment {
        BACKEND_IMAGE = "${params.DOCKERHUB_USER}/achat-backend"
        FRONTEND_IMAGE = "${params.DOCKERHUB_USER}/achat-frontend"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
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
        stage('BACKEND TESTS') {
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
        stage('FRONTEND TESTS') {
            steps {
                dir('frontend') {
                    sh '''
                        npm ci --no-audit --no-fund
                        npm test -- --watch=false
                    '''
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
                // Publish the same JAR under fixed release coordinates for the demo.
                // maven-releases must allow redeploy so subsequent builds can overwrite it.
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: params.NEXUS_HOST,
                    groupId: 'org.example',
                    version: '0.0.1',
                    repository: 'maven-releases',
                    credentialsId: 'nexus-credentials',
                    artifacts: [
                        [artifactId: 'achat', classifier: '', file: 'backend/target/achat-0.0.1-SNAPSHOT.jar', type: 'jar']
                    ]
                )
            }
        }
        stage('DOCKER BUILD') {
            when { expression { params.DOCKER_PUSH } }
            steps {
                script {
                    if (!params.DOCKERHUB_USER?.trim()) {
                        error 'DOCKERHUB_USER est obligatoire pour construire les images'
                    }
                }
                sh '''
                    docker buildx build --load \
                      -t "$BACKEND_IMAGE:$IMAGE_TAG" \
                      -t "$BACKEND_IMAGE:latest" \
                      backend
                    docker buildx build --load \
                      -t "$FRONTEND_IMAGE:$IMAGE_TAG" \
                      -t "$FRONTEND_IMAGE:latest" \
                      frontend
                '''
            }
        }
        stage('DOCKER PUSH') {
            when { expression { params.DOCKER_PUSH } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKERHUB_LOGIN', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''
                        echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_LOGIN" --password-stdin
                        docker push "$BACKEND_IMAGE:$IMAGE_TAG"
                        docker push "$BACKEND_IMAGE:latest"
                        docker push "$FRONTEND_IMAGE:$IMAGE_TAG"
                        docker push "$FRONTEND_IMAGE:latest"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }



                stage('TERRAFORM') {
            when { expression { params.TERRAFORM_ACTION != 'none' } }
            steps {
                withCredentials([file(credentialsId: 'aws-learner-lab', variable: 'AWS_SHARED_CREDENTIALS_FILE')]) {
                    dir('terraform') {
                        sh '''
                            export AWS_DEFAULT_REGION="$AWS_REGION"
                            terraform init -input=false
                            case "$TERRAFORM_ACTION" in
                              plan)    terraform plan -input=false ;;
                              apply)   terraform apply -input=false -auto-approve ;;
                              destroy) terraform destroy -input=false -auto-approve ;;
                            esac
                        '''
                    }
                }
            }
        }

        
        stage('DEPLOY TO EKS') {
            when { expression { params.DOCKER_PUSH && params.DEPLOY_K8S } }
            steps {
                withCredentials([
                    file(credentialsId: 'aws-learner-lab', variable: 'AWS_SHARED_CREDENTIALS_FILE'),
                    usernamePassword(credentialsId: 'achat-db-credentials', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD'),
                    string(credentialsId: 'achat-mysql-root', variable: 'MYSQL_ROOT_PASSWORD')
                ]) {
                    sh '''
                        export KUBECONFIG="$WORKSPACE/.kube/config"
                        aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER"

                        # kubectl gives up after a 10 s TLS handshake; retry on slow networks.
                        kc() {
                          for attempt in 1 2 3 4 5; do
                            kubectl "$@" && return 0
                            echo "kubectl failed (attempt $attempt/5), retrying in 10s..."
                            sleep 10
                          done
                          return 1
                        }

                        # Render everything to files first so a retry can re-read them.
                        DEPLOY_DIR="$WORKSPACE/.deploy"
                        rm -rf "$DEPLOY_DIR"
                        mkdir -p "$DEPLOY_DIR"
                        trap 'rm -rf "$DEPLOY_DIR"' EXIT

                        (umask 077; kubectl -n achat create secret generic achat-db \
                          --from-literal=MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
                          --from-literal=DB_USERNAME="$DB_USERNAME" \
                          --from-literal=DB_PASSWORD="$DB_PASSWORD" \
                          --dry-run=client -o yaml > "$DEPLOY_DIR/secret.yaml")

                        # Point the manifests at the images pushed by this build.
                        for f in k8s/*.yaml; do
                          sed -e "s#DOCKERHUB_USER/achat-backend:latest#$BACKEND_IMAGE:$IMAGE_TAG#" \
                              -e "s#DOCKERHUB_USER/achat-frontend:latest#$FRONTEND_IMAGE:$IMAGE_TAG#" \
                              "$f" > "$DEPLOY_DIR/$(basename "$f")"
                        done

                        kc apply -f "$DEPLOY_DIR/namespace.yaml"
                        kc apply -f "$DEPLOY_DIR/secret.yaml"
                        kc apply -f "$DEPLOY_DIR"

                        # Alloy reads its ConfigMap at startup; pick up config changes on redeploy.
                        kc -n achat rollout restart deployment/alloy
                        kc -n achat rollout status deployment/grafana-lgtm --timeout=10m
                        kc -n achat rollout status deployment/alloy --timeout=5m
                        kc -n achat rollout status deployment/mysql --timeout=5m
                        kc -n achat rollout status deployment/backend --timeout=10m
                        kc -n achat rollout status deployment/frontend --timeout=5m
                        kc -n achat get pods,svc

                        # Pod readiness does not guarantee AWS has assigned a public hostname yet.
                        echo "Waiting for the AWS load balancer hostname..."
                        kubectl -n achat wait service/frontend \
                          --for=jsonpath='{.status.loadBalancer.ingress[0].hostname}' --timeout=5m
                        FRONTEND_HOST=$(kubectl -n achat get svc frontend --request-timeout=30s \
                          -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
                        if [ -z "$FRONTEND_HOST" ]; then
                          echo "AWS load balancer hostname is empty" >&2
                          exit 1
                        fi
                        echo "Application: http://$FRONTEND_HOST"
                        echo "Grafana: run kubectl -n achat port-forward svc/grafana-lgtm 3001:3000, then open http://localhost:3001"
                        echo "Swagger: run kubectl -n achat port-forward svc/backend 8090:8089, then open http://localhost:8090/swagger-ui/index.html"
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                def recipients = params.EMAIL_TO?.trim()
                if (recipients) {
                    // A mail delivery error must not replace the original CI result.
                    catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS', catchInterruptions: false,
                               message: 'Email notification failed; keeping the original build result.') {
                        emailext(
                            to: recipients,
                            subject: "[${currentBuild.currentResult}] ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/plain',
                            body: """Result: ${currentBuild.currentResult}
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Branch: ${params.BRANCH}

Build details: ${env.BUILD_URL}
Console output: ${env.BUILD_URL}console
"""
                        )
                    }
                } else {
                    echo 'Email notification skipped: EMAIL_TO is empty.'
                }
            }
        }
    }
}
