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

  pwd
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
  git clone git://git.eclipse.org/gitroot/www.eclipse.org/concierge
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
  git commit -m "Updated website from GitHub at $now"
  git log -2
  exit 0
fi

if [ "$1" == "push" ] ; then
  cd $REPO_DIR/concierge
  git push
  exit 0
fi
