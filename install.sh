#!/bin/bash

set -e

OSTYPE=$(uname)
BIN_SUFFIX=""
ARCH=""

if [[ "${OSTYPE}" == "Darwin" ]]; then
  BIN_SUFFIX="macos"
elif [[ "${OSTYPE}" == "Linux" ]]; then
  BIN_SUFFIX="linux"
else
  echo "error: The OS is not supported!"
  exit 1
fi

case "$(uname -m)" in
  'amd64' | 'x86_64')
    ARCH='x86_64'
    ;;
  'arm64' | 'aarch64')
    ARCH='arm64'
    ;;
  *)
    echo "error: The architecture is not supported!"
    exit 1
    ;;
esac

if [[ "${BIN_SUFFIX}" == "" || ${ARCH} == "" ]]; then
  echo "error: The OS or architecture is not supported!"
  exit 1
fi

RELEASE=$(curl -s https://api.github.com/repos/gissily/versions-tools/releases/latest  | grep tag_name | cut -d '"' -f 4)

if [[ "${RELEASE}" == "" ]]; then
  echo "fetching latest version failure"
  exit 1
fi

RELEASE_KEY="https://github.com/gissily/versions-tools/releases/download/${RELEASE}/Release.asc"
DOWNLOAD_URL="https://github.com/gissily/versions-tools/releases/download/${RELEASE}/versions-${BIN_SUFFIX}-${ARCH}"
DOWNLOAD_URL_ASC="https://github.com/gissily/versions-tools/releases/download/${RELEASE}/versions-${BIN_SUFFIX}-${ARCH}.asc"
 
echo "downloading ${RELEASE_KEY}"
curl -L "${RELEASE_KEY}" -o /tmp/Release.asc

echo "downloading ${DOWNLOAD_URL}"
curl -L "${DOWNLOAD_URL}" -o /tmp/versions-${BIN_SUFFIX}

echo "downloading ${DOWNLOAD_URL_ASC}"
curl -L "${DOWNLOAD_URL_ASC}" -o /tmp/versions-${BIN_SUFFIX}.asc

gpg --import /tmp/Release.asc
gpg --verify /tmp/versions-${BIN_SUFFIX}.asc /tmp/versions-${BIN_SUFFIX}

chmod +x /tmp/versions-${BIN_SUFFIX}

cp /tmp/versions-${BIN_SUFFIX} /usr/local/bin/versions

echo "versions [${RELEASE}] installed successfully!"

rm -f /tmp/Release.asc
rm -f /tmp/versions-${BIN_SUFFIX}
rm -f /tmp/versions-${BIN_SUFFIX}.asc