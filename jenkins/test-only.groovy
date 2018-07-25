utils.ircNotification([stage: 'Test', status: 'starting'])

env.GIT_COMMIT = "latest"

if ( config.apps ) {
    milestone()
    tested_apps = []
    appURL = config.app_url
    stageName = "test ${appURL}"
    if ( config.integration_tests ) {
        // queue up test closures
        def allTests = [:]
        def tests = config.integration_tests
        for (filename in tests) {
            allTests[filename] = utils.integrationTestJob(filename, appURL)
        }
        stage ("Test ${appURL}") {
            stash 'workspace'
            try {
                // wait for server to be ready
                sleep(time: 10, unit: 'SECONDS')
                if ( allTests.size() == 1 ) {
                    allTests[tests[0]]()
                } else {
                    parallel allTests
                }
            } catch(err) {
                utils.ircNotification([stage: "Integration Tests ${appURL}", status: 'failure'])
                throw err
            }
            tested_apps << "${appURL}".toString()
        }
    }
}
if ( tested_apps ) {
    // huge success \o/
    utils.ircNotification([message: "All tests passed: ${tested_apps.join(', ')}", status: 'success'])
}
