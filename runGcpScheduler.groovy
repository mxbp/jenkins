import groovy.transform.Field

/*
    Calling GCP scheduler via API
*/

pipeline {
    agent {
        label 'DinD'
    }
    triggers {
        cron 'H 17 * * 1-5'
    }
    stages {
        stage ('Run Cloud Scheduler') {
            steps {
                script {
                    cloudScheduler.each {
                        httpRequest(
                            authentication: 'token', // TODO: add cred user/pass with token
                            httpMode: 'POST',
                            url: 'https://cloudscheduler.googleapis.com/v1/projects/' + it.projectGCP +
                                '/locations/' + it.locationGCP +
                                '/jobs/' + it.jobGCP + ':run'
                        )
                    }
                }
            }
        }
    }
}

class gcpScheduler {
    String projectGCP, locationGCP, jobGCP
    gcpScheduler (projectGCP, locationGCP, jobGCP) {
        this.projectGCP = projectGCP; this.locationGCP = locationGCP; this.jobGCP = jobGCP
    }
}

@Field def cloudScheduler = [
    new gcpScheduler("PROJECT_NAME", "LOCATION_NAME", "JOB_NAME_1"),
    new gcpScheduler("PROJECT_NAME", "LOCATION_NAME", "JOB_NAME_2")
]
