pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'your-docker-registry.com'
        IMAGE_NAME = 'be-payment-service'
        KUBECONFIG = credentials('kubeconfig')
        ARGOCD_SERVER = 'your-argocd-server.com'
        ARGOCD_TOKEN = credentials('argocd-token')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.BUILD_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew clean test'
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'build/test-results/test/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }
        
        stage('Build Application') {
            steps {
                sh './gradlew clean build -x test'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    def image = docker.build("${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}")
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }
        
        stage('Update Manifest') {
            steps {
                script {
                    sh """
                        git config user.email "jenkins@company.com"
                        git config user.name "Jenkins CI"
                        
                        # Update image tag in k8s manifests
                        sed -i 's|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:.*|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}|g' k8s/deployment.yaml
                        
                        # Commit and push changes
                        git add k8s/deployment.yaml
                        git commit -m "Update image tag to ${BUILD_TAG}"
                        git push origin main
                    """
                }
            }
        }
        
        stage('Sync ArgoCD') {
            steps {
                script {
                    sh """
                        argocd login ${ARGOCD_SERVER} --auth-token ${ARGOCD_TOKEN} --insecure
                        argocd app sync payment-service-app
                        argocd app wait payment-service-app --health
                    """
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend(
                channel: '#deployments',
                color: 'good',
                message: "✅ Payment Service deployed successfully: ${BUILD_TAG}"
            )
        }
        failure {
            slackSend(
                channel: '#deployments',
                color: 'danger',
                message: "❌ Payment Service deployment failed: ${BUILD_TAG}"
            )
        }
    }
}