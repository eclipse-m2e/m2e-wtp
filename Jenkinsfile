def boolean useCredentials = false

pipeline {
  agent {
    label 'centos-latest'
   }

  options {
    timestamps()
    timeout(time: 45, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds(abortPrevious: true)
  }

  tools {
    maven 'apache-maven-latest'
    jdk 'temurin-jdk21-latest'
  }

  parameters {
    choice(
      name: 'BUILD_TYPE',
      choices: ['nightly', 'milestone', 'release'],
      description: '''
        Choose the type of build.
        Note that a release build will not promote the build, but rather will promote the most recent milestone build.
        '''
    )

    booleanParam(
      name: 'PROMOTE',
      defaultValue: true,
      description: 'Whether to promote the build to the download server.'
    )
  }

  stages {
    stage('Display Parameters') {
      steps {
        script {
          def description = """
BUILD_TYPE=${params.BUILD_TYPE}
BRANCH_NAME=${env.BRANCH_NAME}
PROMOTE=${params.PROMOTE}
""".trim()
          echo description
          currentBuild.description = description.replace("\n", "<br/>")
          env.BUILD_TYPE = params.BUILD_TYPE
          if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == null) {
            useCredentials = true
            if (params.PROMOTE) {
              env.SIGN = true
              env.PROMOTE = true
            } else {
              env.SIGN = false
              env.PROMOTE = false
            }
          } else {
            useCredentials = false
            env.SIGN = false
            env.PROMOTE = false
          }
        }
      }
    }

    stage('Build') {
      steps {
        script {
          wrap([$class: 'Xvnc', useXauthority: true]) {
            if (useCredentials) {
              sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                mvn()
              }
            } else {
              mvn()
            }
          }
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'org.eclipse.*.site/target/repository/**/*,org.eclipse.*.site/target/*.zip,*/target/work/data/.metadata/.log,*/target/work/configuration/**'
      junit '*/target/surefire-reports/TEST-*.xml,*/*/target/surefire-reports/TEST-*.xml'
    }

    failure {
      mail to: 'ed.merks@gmail.com',
      subject: "[m2e WTP] Build Failure ${currentBuild.fullDisplayName}",
      mimeType: 'text/html',
      body: "Project: ${env.JOB_NAME}<br/>Build Number: ${env.BUILD_NUMBER}<br/>Build URL: ${env.BUILD_URL}<br/>Console: ${env.BUILD_URL}/console"
    }

    fixed {
      mail to: 'ed.merks@gmail.com',
      subject: "[m2e WTP] Back to normal ${currentBuild.fullDisplayName}",
      mimeType: 'text/html',
      body: "Project: ${env.JOB_NAME}<br/>Build Number: ${env.BUILD_NUMBER}<br/>Build URL: ${env.BUILD_URL}<br/>Console: ${env.BUILD_URL}/console"
    }

    cleanup {
      deleteDir()
    }
  }
}

def void mvn() {
  sh '''
    pwd
    if [[ $PROMOTE == false ]]; then
      sign_argument='-Pci'
    else
      sign_argument='-Peclipse-sign,promote,ci'
    fi
    mvn \
      --no-transfer-progress\
      -Dorg.eclipse.justj.p2.manager.build.url=$JOB_URL \
      -Dbuild.type=$BUILD_TYPE \
      -Dgit.commit=$GIT_COMMIT \
      -DskipTests=false \
      -Dmaven.repo.local=$WORKSPACE/.m2/repository \
      -Dtycho.surefire.timeout=720 \
      $sign_argument \
      clean \
      verify
    '''
}