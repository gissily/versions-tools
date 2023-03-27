#!/bin/bash


RELEASE=$(curl -s https://api.github.com/repos/gissily/versions-tools/releases/latest  | grep tag_name | cut -d '"' -f 4)

DOWNLOAD_URL="https://github.com/gissily/versions-tools/releases/download/${RELEASE}/versions"
 
echo "downloading ${DOWNLOAD_URL}"

curl -L ${DOWNLOAD_URL} -o /tmp/versions 

chmod +x /tmp/versions

cp /tmp/versions /usr/local/bin/versions

rm -f /tmp/versions