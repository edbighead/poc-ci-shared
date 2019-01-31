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
    
    def sonar(projectKey,exclusions) {
        script.withSonarQubeEnv('sonar') {
            mvn ("sonar:sonar -Dsonar.projectKey=${projectKey} \
                              -Dsonar.exclusions=${exclusions}")
        }
    }

    def qualityGate() {
        def qualitygate = script.waitForQualityGate()
        if (qualitygate.status != "OK") {
         script.error "Pipeline aborted due to quality gate status: ${qualitygate.status}"
      }
    }

    def setVersion (major,minor,incremental) {
        if (major) {
            mvn("build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.nextMajorVersion}.0.0 versions:commit")
        }

        if (minor) {
            mvn("build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.nextMinorVersion}.0 versions:commit")
        }

        if (incremental) {
            mvn("build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} versions:commit")
        }

    }

    def pushIncrementedVersion(branch){
        script.sshagent(['github-key']) {
            script.sh ("git add . && git commit -m 'Increment version' && git push --set-upstream origin ${branch}")
            script.pom = script.readMavenPom file: 'pom.xml'
            script.sh ("git tag ${script.pom.version} && git push --tags")
        }
    }

    def download(groupid,artifactid,version,ext,dest){
        mvn("dependency:get -DremoteRepositories=http://nexus:8081/repository/maven-releases -DgroupId=${groupid} -DartifactId=${artifactid} -Dversion=${version} -Dpackaging=${ext} -Dtransitive=false -Ddest=${dest}")
    }

    def downloadHelloService (version) {
        download("com.boxfuse.samples", "hello", version, "war", "deploy/hello.war")
    }

    def deploy(env,service){
        mvn("install -Pweblogic,${env},${service}")
    }

    def deployHelloService(env){
        deploy(env,"service-1")
    }



}
