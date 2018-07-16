milestone()
stage ('Build Images') {
    // make sure we should continue
    if ( config.require_tag ) {
        try {
            sh 'docker/bin/check_if_tag.sh'
        } catch(err) {
            utils.ircNotification([stage: 'Git Tag Check', status: 'failure'])
            throw err
        }
    }
    utils.ircNotification([stage: 'Test & Deploy', status: 'starting'])
    lock ("bedrock-docker-build") {
        try {
            sh "make clean build-ci"
        } catch(err) {
            utils.ircNotification([stage: 'Docker Build', status: 'failure'])
            throw err
        }
        // save the files for later
        stash 'workspace'
    }
}

if ( config.smoke_tests ) {
    milestone()
    stage ('Test Images') {
        try {
            parallel([
                smoke_tests: utils.integrationTestJob('smoke'),
                unit_tests: {
                    node {
                        unstash 'workspace'
                        sh 'make test-ci'
                    }
                },
            ])
        } catch(err) {
            utils.ircNotification([stage: 'Unit Test', status: 'failure'])
            throw err
        }
    }
}

// test this way to default to true for undefined
if ( config.push_public_registry != false ) {
    milestone()
    stage ('Push Public Images') {
        try {
            if (config.demo) {
                utils.pushDockerhub('mozorg/bedrock')
            }
            else {
                utils.pushDockerhub('mozorg/bedrock_test')
                utils.pushDockerhub('mozorg/bedrock_assets')
                utils.pushDockerhub('mozorg/bedrock_code')
                utils.pushDockerhub('mozorg/bedrock_build')
                utils.pushDockerhub('mozorg/bedrock')
            }
        } catch(err) {
            utils.ircNotification([stage: 'Dockerhub Push Failed', status: 'failure'])
            throw err
        }
    }
}

if ( config.apps ) {
    tested_apps = []
    // default to oregon-b only
    def regions = config.regions ?: ['oregon-b']
    def deployments = [:]
    for (appname in config.apps) {
        for (regionId in regions) {
            def region = global_config.regions[regionId]
            deployments[regionId] = utils.deploymentJob(config, region, appname, tested_apps)
            if (deployments.size() == 1) {
                deployments[regionId[0]]()
            } else {
                parallel deployments
            }
        }
    }
    if ( tested_apps ) {
        // huge success \o/
        utils.ircNotification([message: "All tests passed: ${tested_apps.join(', ')}", status: 'success'])
    }
}
