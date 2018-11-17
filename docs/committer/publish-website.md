# How to publish website

The [Eclipse Concierge Website](http://eclipse.org/concierge) will be published from Git repository at `https://git.eclipse.org/r/a/www.eclipse.org/concierge`.

This repository is meanwhile only writable through Gerrit. Project committers are allowed to write directly to master, which will then be published to Eclipse Webserver within short time frame.

As the maintenance of the website in a different repository is error-prone, we will maintain the website now from main repository at GitHub.

Parts of documentation is even shared (e.g. all Markdown files). So website can be generated and then easily published to Gerrit repository.

## How to publish

Note: you need committer rights to be able to push to Gerrit repo.

You need to configure your credentials to be able to push to Gerrit.
Most easy way is to configure in `~/.gradle/gradle.properties` your credentials:

```
// access to Concierge Gerrit repo
conciergeGerritUsername=<your-username>
conciergeGerritPassword=<your-http-password>
```

Note: Gerrit password is the HTTP password which can be generated at [https://git.eclipse.org/r/#/settings/http-password](https://git.eclipse.org/r/#/settings/http-password).

There is a helper script to publish the website. Run it from root directory of Concierge project:

```
# will clean all generated files
./distribution/publish/publishWebsite.sh clean

# will clone current repo and update it with generated website
./distribution/publish/publishWebsite.sh prepare

# git status shows all changes which can be checked manually

# will commit all changes to repo 
./distribution/publish/publishWebsite.sh commit

# will push master to Gerrit repo
./distribution/publish/publishWebsite.sh push

# clean all generated files when done
./distribution/publish/publishWebsite.sh clean
```

There is a CI process which does that by running a Jenkins job. See [https://ci.eclipse.org/concierge/view/CI_CD/job/Master_PublishWebsite/](https://ci.eclipse.org/concierge/view/CI_CD/job/Master_PublishWebsite/)

