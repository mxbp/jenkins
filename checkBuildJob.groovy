import groovy.transform.Field

/*
    Checking that the jobs were launched today
    If there was no launch today, then run the jobs

    PS. If the jobs being checked are currently running, then we wait for them to complete
*/

def jobRun = []

pipeline {
    agent {
        label 'DinD'
    }
    environment {
        JOB_NAME = "PATH/JOB_1,PATH/JOB_2"
    }

    triggers {
        cron 'H 17 * * 1-5'
    }
    stages {
        stage('Get status jobs') {
            steps {
                script {
                    def jobStage = [:]
                    def currentDate = new Date().format("yyyyMMdd")
                    
                    ESS_JOB_NAME.split(',').each { jobPath ->
                        jobStage[jobPath] = {
                            // Wait until the job is completed 
                            waitUntil(initialRecurrencePeriod: 5000, quiet: true) {
                                def job = Jenkins.getInstance().getItemByFullName(jobPath)
                                def wait = !(job.isBuilding() || job.isInQueue())
                                println('wait - ' + wait)
                                return wait == true
                            }
                            def job = Jenkins.getInstance().getItemByFullName(jobPath)
                            def buildLastNumber = job.getLastBuild().number
                            def buildDate = job.getBuildByNumber(buildLastNumber).getTime().format("yyyyMMdd")
                            
                            // Checking that the job was ran today
                            if (currentDate.toString() == buildDate.toString()) {
                                def buildStatus = job.getBuildByNumber(buildLastNumber).getResult()
                                def message = "Built today " + jobPath + " with " + buildStatus + " status"
                                
                                // Checking status job
                                if (buildStatus.toString() == "SUCCESS") {
                                    println(message)
                                } else {
                                    println(message)
                                    jobRun.add(jobPath)
                                }
                            // Job didn't run today
                            } else {
                                println("Need to run the job! - " + jobPath)
                                jobRun.add(jobPath)
                            }
                        }
                    }
                    parallel jobStage
                }
            }
        }
        stage("Run Jobs") {
            when {
                expression { jobRun != [] }
                beforeAgent true
            }
            steps {
                script {
                    def jobStage = [:]
                    jobRun.each { jobPath ->
                        jobStage[jobPath] = {
                            stage("Run " + jobPath) {
                                // Run job
                                build job: jobPath
                                    parameters: [
                                        string(name: 'PARAMETER_1', value: 'VALUE_1'),
                                        booleanParam(name: 'PARAMETER_2', value: false)
                                    ]
                            }
                        }
                    }
                    parallel(jobStage)
                }
            }
        }
    }
}
