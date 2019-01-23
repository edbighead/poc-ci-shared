package com.library

class Shared implements Serializable{
    def script
    def repoOwner = "edbighead"

    Shared(script) {this.script = script}

    def getJsonBody(state) {
        def jsonBody = """{ "state": "${state}", "target_url": "${script.env.BUILD_URL}", "description": "Build ${state}", "context": "continuous-integration/jenkins"}"""

        return jsonBody
    }
 
    def setGithubStatus(repo, status) {
        def response = script.httpRequest httpMode: 'POST', 
        customHeaders: [[name: "Authorization", value: "token ${script.env.GITHUB_TOKEN}"],[name: "Content-Type", value: "application/json"]], 
        url: "https://api.github.com/repos/${repoOwner}/${repo}/statuses/${script.env.GIT_COMMIT}",
        requestBody: getJsonBody(status)
    }

    def mvn(goal) {
        script.withMaven( maven: 'maven-3.6.0', mavenSettingsConfig: 'my-maven-settings' ) {
            script.sh "mvn ${goal}"
        }
    }

    def setVersion (major,minor,incremental) {
        if (major) {
            mvn("build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.nextMajorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.incrementalVersion} versions:commit")
        }

        if (minor) {
            mvn("build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.nextMinorVersion}.\\\${parsedVersion.incrementalVersion} versions:commit")
        }

        if (incremental) {
            mvn("build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} versions:commit")
        }

    }
}