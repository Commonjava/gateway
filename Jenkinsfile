def artifact="target/*.jar"
def artifact_runner="target/*-runner.jar"

def ocp_map = '/mnt/ocp/jenkins-openshift-mappings.json'
def bc_section = 'build-configs'

def my_bc = null

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
                sh '${M2_HOME}/bin/mvn -B -V clean verify -Prun-its -Pci'
            }
        }
        stage('Load OCP Mappings') {
            when {
                allOf {
                    expression { env.CHANGE_ID == null } // Not pull request
                }
            }
            steps {
                echo "Load OCP Mapping document"
                script {
                    def exists = fileExists ocp_map
                    if (exists){
                        def jsonObj = readJSON file: ocp_map
                        if (bc_section in jsonObj){
                            if (env.GIT_URL in jsonObj[bc_section]) {
                                echo "Found BC for Git repo: ${env.GIT_URL}"
                                if (env.BRANCH_NAME in jsonObj[bc_section][env.GIT_URL]) {
                                    my_bc = jsonObj[bc_section][env.GIT_URL][env.BRANCH_NAME]
                                } else {
                                    my_bc = jsonObj[bc_section][env.GIT_URL]['default']
                                }
                                echo "Using BuildConfig: ${my_bc}"
                            }
                            else {
                                echo "Git URL: ${env.GIT_URL} not found in BC mapping."
                            }
                        }
                        else {
                            "BC mapping is invalid! No ${bc_section} sub-object found!"
                        }
                    }
                    else {
                        echo "JSON configuration file not found: ${ocp_map}"
                    }
                }
            }
        }
        stage('Deploy') {
            when { branch 'master' }
            steps {
                echo "Deploy"
                sh '${M2_HOME}/bin/mvn help:effective-settings -B -V clean deploy -e'
            }
        }
        stage('Archive') {
            steps {
                echo "Archive"
                archiveArtifacts artifacts: "$artifact", fingerprint: true
            }
        }
        stage('Build & Push Image') {
            when {
                allOf {
                    expression { my_bc != null }
                    expression { env.CHANGE_ID == null } // Not pull request
                }
            }
            steps {
                script {
                    def artifact_runner_file = sh(script: "ls $artifact_runner", returnStdout: true)?.trim()
                    def tarball_url = "${BUILD_URL}artifact/$artifact_runner_file"
                    openshift.withCluster() {
                        openshift.withProject() {
                            echo "Starting image build: ${openshift.project()}:${my_bc}"
                            def bc = openshift.selector("bc", my_bc)
                            def buildSel = bc.startBuild("-e tarball_url=${tarball_url}")
                            buildSel.logs("-f")
                        }
                    }
                }
            }
        }
    }
}
