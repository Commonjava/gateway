pipeline {
    agent { label 'maven-36-jdk11' }
    stages {
        stage('Prepare') {
            steps {
                sh 'printenv'
            }
        }
        stage('Build') {
            when {
                expression { env.CHANGE_ID != null } // Pull request
            }
            steps {
                sh 'ls -l /home/jenkins'
                sh 'ls -l /home/jenkins/apache-maven-3.6.3'
                sh 'ls -l /home/jenkins/apache-maven-3.6.3/bin'
                sh '/home/jenkins/apache-maven-3.6.3/bin/mvn -B -V clean verify -Prun-its -Pci'
            }
        }
        stage('Deploy') {
            when { branch 'master' }
            steps {
                echo "Deploy"
                sh '${M2_HOME}/bin/mvn help:effective-settings -B -V clean deploy -e'
            }
        }
    }
}
