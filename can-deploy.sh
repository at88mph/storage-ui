#! /bin/bash

if [[ "${TRAVIS_BRANCH}" = "master" && "${TRAVIS_PULL_REQUEST}" = "false" ]];
then
    echo "DEPLOY";
fi