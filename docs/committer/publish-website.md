# How to publish website

The Eclipse Concierge Website (http://eclipse.org/concierge) will be published from Git repository at `https://jhiller@git.eclipse.org/r/a/www.eclipse.org/concierge`.

This repository is meanwhile only writable through Gerrit. Project committers are allowed to write directly to master, which will then be published to Eclipse Webserver within short time frame.

As the maintenance of the website in a different repository is error-prone, we will maintain the website now from main repository at GitHub.

Parts of documentation is even shared (e.g. all Markdown files). So website can be generated and then easily published to Gerrit repository.

## How to publish

There is a helper script to public the website:

```
# will clean all generated files
./distribution/publish/publishWebsite.sh clean

# will clone current repo and update it with generated website
./distribution/publish/publishWebsite.sh prepare

# now the changes can be checked manually

# will commit all changes to repo 
./distribution/publish/publishWebsite.sh commit

# will push master to Gerrit repo
./distribution/publish/publishWebsite.sh push

# clean all generated files when done
./distribution/publish/publishWebsite.sh clean
```

Note: you need committer rights to be able to push to Gerrit repo.
