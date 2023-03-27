#!/bin/bash

set -e

SCRIPT=`readlink -f "${BASH_SOURCE:-$0}"`
SCRIPT_DIR_PATH=`dirname ${SCRIPT}`
CI_DIR_PATH=`dirname ${SCRIPT_DIR_PATH}`
ROOT_PATH=`dirname ${CI_DIR_PATH}`

VERSION=$1

echo ${VERSION}
${ROOT_PATH}/mvnw -U clean compile >> /dev/null 2>&1

${ROOT_PATH}/mvnw versions:set -DnewVersion=${VERSION}
