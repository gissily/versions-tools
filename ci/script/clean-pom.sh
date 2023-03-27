#!/bin/bash

SCRIPT=`readlink -f "${BASH_SOURCE:-$0}"`
SCRIPT_DIR_PATH=`dirname ${SCRIPT}`
CI_DIR_PATH=`dirname ${SCRIPT_DIR_PATH}`
ROOT_PATH=`dirname ${CI_DIR_PATH}`

find ${ROOT_PATH}/ -type f -iname '.flattened-pom.xml' | xargs -I {} rm -f {};
find ${ROOT_PATH}/ -type f -iname 'pom.xml.versionsBackup' | xargs -I {} rm -f {};
