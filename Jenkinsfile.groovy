@Library('semantic_releasing') _

podTemplate(label: 'mypod', containers: [
        containerTemplate(name: 'docker', image: 'docker', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.0', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'curl', image: 'khinkali/jenkinstemplate:0.0.3', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'maven', image: 'maven:3.5.2-jdk-8', command: 'cat', ttyEnabled: true)
],
        volumes: [
                hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
        ]) {
    try {
        node('mypod') {
            properties([
                    buildDiscarder(
                            logRotator(artifactDaysToKeepStr: '',
                                    artifactNumToKeepStr: '',
                                    daysToKeepStr: '',
                                    numToKeepStr: '30'
                            )
                    ),
                    pipelineTriggers([cron('30 2 * * *')])
            ])

            stage('create backup') {
                currentBuild.displayName = getTimeDateDisplayName()

                def kc = 'kubectl -n test'
                def containerPath = '/var/lib/postgresql/data'
                def containerName = 'kafka-backup-db'
                def podLabel = 'app=kafka-backup-db'
                def repositoryUrl = 'bitbucket.org/khinkali/kafka_backup_db_test'
                container('kubectl') {
                    backup(podLabel, containerName, containerPath, repositoryUrl, kc)
                }
            }

        }
    } catch (all) {
        slackSend channel: '#jenkins',
                color: 'good',
                message: "Build Failed - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)",
                teamDomain: 'khikali',
                token: 'slack-token'
    }
}