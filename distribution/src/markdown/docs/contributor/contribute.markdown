## Contributing

Contributions to Concierge are always welcome!

### Mailing List

A great way to stay up to date with Concierge activity is to subscribe to the Concierge email list provided by Eclipse. Sign up for the mailing list [here](https://dev.eclipse.org/mailman/listinfo/concierge-dev).

### Issue Tracker

Issues and bugs related to Concierge are tracked with the Eclipse Bugzilla tracking system. Please enter any issues you find [there](https://bugs.eclipse.org/bugs/buglist.cgi?component=core&list_id=9301172&product=Concierge).

### Contributing

If you want to contribute code to the Concierge project via Git, follow the [Eclipse contribution process](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git).

#### "How-To-Contribute" Tutorial using Eclipse IDE

* Make sure to have an account in Gerrit with Contributor License Agreement (CLA)
* Goto Git perspective
* Clone repo from https://git.eclipse.org/r/concierge/org.eclipse.concierge
** use your Gerrit account for authentication
* Configure Git repo for your Name/Email address from your CLA
* Import project(s) you are going to work on
* Create a branch to commit your changes
* Checkout your branch
* Work on your code
* Goto Git perspective
* Commit your changes, signoff with your Name/Email address from your CLA and make sure you have a "Change-Id: 000" included
* "Push to Gerrit", either to master (refs/for/master) or to a specific branch (/refs/for/R6_dev)

#### "How-To-Contribute" using git command line tool

* Make sure to have an account in Gerrit with Contributor License Agreement (CLA)
* Check out Concierge project
* Create a branch to commit your changes
* Checkout your branch
* Work on your code
* Commit your changes, signoff with your Name/Email address from your CLA
** push either to master (refs/for/master) or to a specific branch (/refs/for/R6_dev)

Sample calls:
```
git clone https://git.eclipse.org/r/concierge/org.eclipse.concierge
cd org.eclipse.concierge
git config --local --add "user.name" "John Doe"
git config --local --add "user.email" "john.doe@example.com"
git branch my-change
git checkout my-change
# change some files
git commit -m "My commit message" -s <files>
git push origin HEAD:refs/for/master
Username for 'https://git.eclipse.org': johndoe
Password for 'https://johndoe@git.eclipse.org':
Counting objects: 3, done.
Delta compression using up to 8 threads.
Compressing objects: 100% (3/3), done.
Writing objects: 100% (3/3), 320 bytes | 0 bytes/s, done.
Total 3 (delta 2), reused 0 (delta 0)
remote: Resolving deltas: 100% (2/2)
remote: Processing changes: new: 1, refs: 1, done
remote: ----------
remote: Reviewing commit: c9fab060
remote: Authored by: John Doe <john.doe@example.com>
remote:
remote: This commit passes Eclipse validation.
remote:
remote: New Changes:
remote:   https://git.eclipse.org/r/60160 My commit message
remote:
To https://git.eclipse.org/r/concierge/org.eclipse.concierge
 * [new branch]      HEAD -> refs/for/master
```

#### "How-To-Review" for Committers

* Check incoming changes at https://git.eclipse.org/r/#/dashboard/self
* Pick one change to review
* Review the changed files, comment on changes
* If review has been done, reply to change
* Select your recommendation between -2,+2 dependent on your decision
* Verify the change: -1,+1
* Post the review
* Afterwards you have to Submit the change to be merged into master
