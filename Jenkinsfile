pipeline {
	agent {
		label 'centos-latest'
	}
	
	options {
		buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
		timestamps()
	}
	
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk17-latest'
	}
	
	environment {
		// Target platform to build against (must correspond to a profile in the parent pom.xml)
		PLATFORM = 'platform-2023-03'
	}
	
	stages {
		stage ('Nightly') {
			when {
				allOf {
					not {
						branch 'PR-*'
					}
					not {
						tag "*"
					}
				}
			}
			steps {
				wrap([$class: 'Xvnc', takeScreenshot: false, useXauthority: true]) {
					sh '''
						xrandr
						xrandr -s 1680x1050
						xsetroot -solid grey
						vncconfig -iconic &
						xhost +
						sleep 2
						metacity --replace --sm-disable --display=${DISPLAY} &
						sleep 2
						mvn clean verify -P$PLATFORM -Psign -Dmaven.wagon.provider.http=httpclient -Dmaven.artifact.threads=12 -Dhttp.tcp.nodelay=false
					'''
				}
				sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						chmod +x ./releng/org.eclipse.acceleo.releng/publish-nightly.sh
						./releng/org.eclipse.acceleo.releng/publish-nightly.sh
					'''
				}
			}
		}
		stage ('Tag') {
			when {
				allOf {
					not {
						branch 'PR-*'
					}
					tag "*"
				}
			}
			steps {
				wrap([$class: 'Xvnc', takeScreenshot: false, useXauthority: true]) {
					sh '''
						xrandr
						xrandr -s 1680x1050
						xsetroot -solid grey
						vncconfig -iconic &
						xhost +
						sleep 2
						metacity --replace --sm-disable --display=${DISPLAY} &
						sleep 2
						mvn clean verify deploy:deploy -P$PLATFORM -Psign -Dmaven.wagon.provider.http=httpclient -Dmaven.artifact.threads=12 -Dhttp.tcp.nodelay=false
					'''
				}
				sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						chmod +x ./releng/org.eclipse.acceleo.releng/publish-nightly.sh
						./releng/org.eclipse.acceleo.releng/publish-nightly.sh
					'''
				}
			}
		}
		stage ('PR Verify') {
			when {
				branch 'PR-*'
			}
			steps {
				wrap([$class: 'Xvnc', takeScreenshot: false, useXauthority: true]) {
					sh '''
						xrandr
						xrandr -s 1680x1050
						xsetroot -solid grey
						vncconfig -iconic &
						xhost +
						sleep 2
						metacity --replace --sm-disable --display=${DISPLAY} &
						sleep 2
						mvn clean verify -P$PLATFORM -Dmaven.wagon.provider.http=httpclient -Dmaven.artifact.threads=12 -Dhttp.tcp.nodelay=false
					'''
				}
			}
		}
	}
	
	post {
		always {
			junit "tests/*.test*/target/surefire-reports/*.xml,query/tests/*.test*/target/surefire-reports/*.xml"
		}
		unsuccessful {
			emailext (
				subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
				body: """FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
				Check console output at ${env.BUILD_URL}""",
				to: 'laurent.goubet@obeo.fr, yvan.lussaud@obeo.fr'
			)
		}
	}
}
