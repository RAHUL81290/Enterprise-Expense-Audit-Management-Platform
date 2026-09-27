pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io/rahulkumar'  // replace with your actual registry namespace
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test — Auth Service') {
            steps {
                dir('auth-service') {
                    sh 'mvn clean package'
                }
            }
            post {
                always {
                    junit 'auth-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build & Test — Expense Service') {
            steps {
                dir('expense-service') {
                    sh 'mvn clean package'
                }
            }
            post {
                always {
                    junit 'expense-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build & Test — Audit Service') {
            steps {
                dir('audit-service') {
                    sh 'mvn clean package'
                }
            }
            post {
                always {
                    junit 'audit-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build & Test — API Gateway') {
            steps {
                dir('api-gateway') {
                    sh 'mvn clean package'
                }
            }
            post {
                always {
                    junit 'api-gateway/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def services = ['auth-service', 'expense-service', 'audit-service', 'api-gateway']
                    services.each { service ->
                        sh "docker build -t ${DOCKER_REGISTRY}/${service}:${IMAGE_TAG} -t ${DOCKER_REGISTRY}/${service}:latest ./${service}"
                    }
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    sh "echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin"

                    def services = ['auth-service', 'expense-service', 'audit-service', 'api-gateway']
                    services.each { service ->
                        sh "docker push ${DOCKER_REGISTRY}/${service}:${IMAGE_TAG}"
                        sh "docker push ${DOCKER_REGISTRY}/${service}:latest"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded - build #${env.BUILD_NUMBER}. Images pushed with tag ${IMAGE_TAG}."
        }
        failure {
            echo "Pipeline failed - check the stage logs above for the first failing step."
        }
        always {
            sh 'docker logout'
        }
    }
}
