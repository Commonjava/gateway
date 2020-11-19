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
                sh '${M2_HOME}/bin/mvn -B -V clean verify -DskipTests -Prun-its -Pci'
            }
        }
        stage('Deploy') {
            when { branch 'master' }
            steps {
                echo "Deploy"
                sh '${M2_HOME}/bin/mvn help:effective-settings -B -V clean deploy -e -DskipTests'
            }
        }
    }
}
