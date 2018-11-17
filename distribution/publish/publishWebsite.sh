#!/bin/bash

# start this script manually:
# chmod u+x ./distribution/publish/publishWebsite.sh
# ./distribution/publish/publishWebsite.sh

# it will clone and update the repo in ./concierge-website-tmp

REPO_DIR=./website-update-tmp

if [ "$#" == "0" ] ; then
  echo "Usage: publishWebsite {clean | prepare | commit | push}"
  echo "         clean: clean all generated files"
  echo "       prepare: clone repo, and update with current website from repo"
  echo "   manual step: check if all changes are OK"
  echo "        commit: add all changes files to stages, commit changes"
  echo "          push: push to origin repo, no review in Gerrit"
  exit 1
fi


if [ "$1" == "clean" ] ; then
  # cleanup tmp directory
  if [ -d $REPO_DIR/concierge ] ; then
    echo "Cleanup $REPO_DIR/concierge ..."
    rm -rf $REPO_DIR/concierge/*
    rm -rf $REPO_DIR/concierge/.git
    rmdir $REPO_DIR/concierge
  fi
  if [ -d $REPO_DIR ] ; then
    echo "Cleanup $REPO_DIR ..."
    rmdir $REPO_DIR
  fi
  exit 0
fi


if [ "$1" == "prepare" ] ; then
  echo "Generating website..."

  (cd distribution ; ../gradlew generateWebsite)
  GENERATED_DIR=`pwd`/distribution/build/website

  # cleanup tmp directory
  if [ -d $REPO_DIR/concierge ] ; then
    echo "Cleanup $REPO_DIR/concierge ..."
    rm -rf $REPO_DIR/concierge/*
    rm -rf $REPO_DIR/concierge/.git
    rmdir $REPO_DIR/concierge
  fi
  mkdir -p $REPO_DIR
  cd $REPO_DIR



  # checkout current repo
  # get credentials, try ~/.gradle/gradle.properties
  if [ "$GERRIT_USERNAME" == "" ]; then
    if [ -f ~/.gradle/gradle.properties ] ; then
      GERRIT_USERNAME=`cat ~/.gradle/gradle.properties | grep "conciergeGerritUsername" | sed -e 's|^conciergeGerritUsername=||g'`
    fi
  fi
  if [ "$GERRIT_PASSWORD" == "" ]; then
    if [ -f ~/.gradle/gradle.properties ] ; then
      GERRIT_PASSWORD=`cat ~/.gradle/gradle.properties | grep "conciergeGerritPassword" | sed -e 's|^conciergeGerritPassword=||g'`
    fi
  fi
  if [ "$GERRIT_AUTHOR" == "" ]; then
    if [ -f ~/.gradle/gradle.properties ] ; then
      GERRIT_AUTHOR=`cat ~/.gradle/gradle.properties | grep "conciergeGerritAuthor" | sed -e 's|^conciergeGerritAuthor=||g'`
    fi
  fi
  if [ "$GERRIT_USERNAME" == "" -o "$GERRIT_PASSWORD" == "" -o "$GERRIT_AUTHOR" == "" ]; then
    echo "Error: you have to set GERRIT_USERNAME, GERRIT_PASSWORD and GERRIT_AUTHOR values, or add"
    echo "    conciergeGerritUsername=your-username"
    echo "    conciergeGerritPassword=your-http-password"
    echo "    conciergeGerritAuthor=Your name <your-email>"
    echo "  to your ~/.gradle/gradle.properties"
    echo "  Note: GERRIT_PASSWORD is the HTTP password which can bet generated at https://git.eclipse.org/r/#/settings/http-password"
    exit 1
  fi
  
  git clone https://$GERRIT_USERNAME:$GERRIT_PASSWORD@git.eclipse.org/r/a/www.eclipse.org/concierge
  cd concierge

  # remove existing files
  rm -rf *.php ./css ./docs ./images ./includes ./repository 

  # copy new website
  cp -r $GENERATED_DIR/* .
  git status

  exit 0
fi

if [ "$1" == "commit" ] ; then
  cd $REPO_DIR/concierge
  now=`date '+%Y-%m-%d %H:%M:%S'`
  git add -A
  # we have to set the author for CI build as it is running with a non-committer user
  git commit -m "Updated website from GitHub at $now" --author "$GERRIT_AUTHOR"
  git log -2
  exit 0
fi

if [ "$1" == "push" ] ; then
  cd $REPO_DIR/concierge
  git push
  exit 0
fi
