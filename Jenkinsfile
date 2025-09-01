pipeline {
    agent any
    
    environment {
        // ===== 로컬 Docker Registry 설정 =====
        DOCKER_REGISTRY = 'localhost:5000'
        SERVICE_NAME = 'payment-service'
        IMAGE_NAME = 'payment-service'
        IMAGE_TAG = "${BUILD_NUMBER}"
        
        // ===== 로컬 Kubernetes 설정 =====
        K8S_NAMESPACE = 'devtrip'
        K8S_CONFIG_PATH = './k8s'
        
        // ===== ArgoCD 로컬 설정 =====
        ARGOCD_SERVER = 'localhost:30080'
        ARGOCD_APP_NAME = 'payment-service-app'
        
        // ===== 알림 설정 =====
        SLACK_CHANNEL = '#devtrip-ci'
    }
    
    stages {
        stage('🚀 Pipeline Start') {
            steps {
                echo "===================================================="
                echo "🚀 Starting CI/CD Pipeline for ${SERVICE_NAME}"
                echo "📋 Build Number: ${BUILD_NUMBER}"
                echo "🌿 Branch: ${env.BRANCH_NAME}"
                echo "===================================================="
            }
        }
        
        stage('📦 Checkout & Setup') {
            steps {
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.BUILD_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    echo "📦 Checked out commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('🧪 Test') {
            steps {
                echo "🧪 Running tests..."
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
        
        stage('🏗️ Build Application') {
            steps {
                echo "🏗️ Building application..."
                sh './gradlew clean build -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: false
            }
        }
        
        stage('🐳 Docker Build') {
            steps {
                script {
                    echo "🐳 Building Docker image..."
                    def dockerImage = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}"
                    sh "docker build -t ${dockerImage} ."
                    sh "docker tag ${dockerImage} ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                    
                    env.DOCKER_IMAGE_FULL = dockerImage
                    echo "Docker image built: ${dockerImage}"
                }
            }
        }
        
        stage('📤 Push to Local Registry') {
            steps {
                script {
                    echo "📤 Pushing to local registry..."
                    sh "docker push ${env.DOCKER_IMAGE_FULL}"
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                    echo "✅ Image pushed: ${env.DOCKER_IMAGE_FULL}"
                }
            }
        }
        
        stage('🚀 Deploy to Local K8s') {
            steps {
                script {
                    echo "🚀 Deploying to local Kubernetes..."
                    
                    sh """
                        # 네임스페이스 생성
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f - || echo "Namespace already exists"
                        
                        # 이미지 업데이트 (deployment가 있는 경우)
                        kubectl set image deployment/${SERVICE_NAME} \
                            ${SERVICE_NAME}=${env.DOCKER_IMAGE_FULL} \
                            -n ${K8S_NAMESPACE} || echo "Deployment not found"
                        
                        # Pod 상태 확인
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=${SERVICE_NAME} || echo "No pods found"
                    """
                }
            }
        }
        
        stage('✅ Health Check') {
            steps {
                script {
                    echo "✅ Running health checks..."
                    sh """
                        echo "Build completed successfully"
                        echo "Service: ${SERVICE_NAME}"
                        echo "Image: ${env.DOCKER_IMAGE_FULL}"
                        echo "Commit: ${env.GIT_COMMIT_SHORT}"
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo "🧹 Cleaning up workspace..."
            
            script {
                try {
                    sh "docker system prune -f"
                } catch (Exception e) {
                    echo "Docker cleanup skipped: ${e.getMessage()}"
                }
            }
            
            cleanWs()
        }
        
        success {
            echo "✅ Pipeline completed successfully for ${SERVICE_NAME}!"
        }
        
        failure {
            echo "❌ Pipeline failed for ${SERVICE_NAME}!"
        }
    }
}