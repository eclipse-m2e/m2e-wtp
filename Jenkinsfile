pipeline {
	options {
		timeout(time: 45, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -f pom.xml -B -Peclipse-sign,uts,its,ci -Dtycho.surefire.timeout=7200 -Dmaven.repo.local=$WORKSPACE/.m2/repository'
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
ssh genie.m2e@projects-storage.eclipse.org "rm -rf /home/data/httpd/download.eclipse.org/technology/m2e/m2e-wtp/snapshots/*" 
scp -r org.eclipse.m2e.wtp.site/target/repository/* genie.m2e@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/technology/m2e/m2e-wtp/snapshots/
					'''
				}
			}
		}
	}
}
