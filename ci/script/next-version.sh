#!/bin/bash

NEXT_DIRECTION=$1

SCRIPT=`readlink -f "${BASH_SOURCE:-$0}"`
SCRIPT_DIR_PATH=`dirname ${SCRIPT}`
CI_DIR_PATH=`dirname ${SCRIPT_DIR_PATH}`
ROOT_PATH=`dirname ${CI_DIR_PATH}`

CURRENT_VERSION=$(${ROOT_PATH}/mvnw help:evaluate -Dexpression=project.version | grep "^[^\\[]" |grep -v Download)
VERSION_NUMBER=${CURRENT_VERSION/-SNAPSHOT/}

echo "current version is [${CURRENT_VERSION}]"

NUMS=($(echo "${VERSION_NUMBER}" | tr '.' '\n'))

NEXT_VERSION=${CURRENT_VERSION}

if [ "${NEXT_DIRECTION}" = "-s" ];then
  s_version=$(echo ${NUMS[2]} + 1 | bc)
  NEXT_VERSION="${NUMS[0]}.${NUMS[1]}.${s_version}-SNAPSHOT"
elif [ "${NEXT_DIRECTION}" = "-n" ];then
  n_version=$(echo ${NUMS[1]} + 1 | bc)
  NEXT_VERSION="${NUMS[0]}.${n_version}.0-SNAPSHOT"
else
  echo "no version next direction"
  exit 1
fi

echo "next version is [${NEXT_VERSION}]"

${SCRIPT_DIR_PATH}/versions-set.sh ${NEXT_VERSION}

echo ${NEXT_VERSION} > /tmp/NEXT_DEVELOPMENT_VERSION
