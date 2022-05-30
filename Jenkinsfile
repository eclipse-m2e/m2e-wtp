pipeline {
	options {
		timeout(time: 180, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk11-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -f pom.xml -B -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Peclipse-sign,uts,its -Dtycho.surefire.timeout=7200'
				}
			}
			post {
				always {
					archiveArtifacts artifacts: 'org.eclipse.*.site/target/repository/**/*,org.eclipse.*.site/target/*.zip,*/target/work/data/.metadata/.log,m2e-core-tests/*/target/work/data/.metadata/.log,m2e-maven-runtime/target/*.properties'
					// Currently, there are no tests for m2e-wtp
					// junit '*/target/surefire-reports/TEST-*.xml,*/*/target/surefire-reports/TEST-*.xml'
				}
			}
		}
		stage('Deploy Snapshot') {
			when {
				branch 'master'
			}
			steps {
				sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
ssh genie.m2e@build.eclipse.org "\
rm -rf  /home/data/httpd/download.eclipse.org/m2e-wtp/snapshots/1.4/* && \
mkdir -p /home/data/httpd/download.eclipse.org/m2e-wtp/snapshots/1.4/m2e-wtp"

# Publish snapshot
scp -r org.eclipse.m2e.wtp.site/target/repository/* genie.m2e@build.eclipse.org:/home/data/httpd/download.eclipse.org/m2e-wtp/snapshots/1.4/m2e-wtp
					'''
				}
			}
		}
	}
}
