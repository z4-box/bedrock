milestone()

utils.slackNotification([stage: 'Test', status: 'starting'])

env.GIT_COMMIT = "latest"

if ( config.apps ) {
    milestone()
    tested_apps = []
    stash 'workspace' // needed by utils.integrationTestJob below
    // default to oregon-b only
    def regions = config.regions ?: ['oregon-b']
    for (regionId in regions) {
        def region = global_config.regions[regionId]
        for (appname in config.apps) {
            appURL = "https://www-akamai.cdn.mozilla.net"
            if ( config.integration_tests ) {
                // queue up test closures
                def allTests = [:]
                def regionTests = config.integration_tests[regionId]
                for (filename in regionTests) {
                    allTests[filename] = utils.integrationTestJob(filename, appURL)
                }
                stage ("Test ${appURL}") {
                    try {
                        if ( allTests.size() == 1 ) {
                            allTests[regionTests[0]]()
                        } else {
                            parallel allTests
                        }
                    } catch(err) {
                        utils.slackNotification([stage: "Integration Tests ${appURL}", status: 'failure'])
                        throw err
                    }
                    tested_apps << "${appURL}".toString()
                }
            }
        }
    }
}
if ( tested_apps ) {
    // huge success \o/
    utils.slackNotification([message: "All tests passed: ${tested_apps.join(', ')}", status: 'success'])
}
