#!/bin/sh
# SCRIPTS FROM https://medium.com/@ranjeetsinha/pre-commit-hooks-what-when-how-522dce4f0b54
echo "Running pre -commit checks..."

#JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
#export JAVA_HOME

#check what environment we're running in
unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*)    machine=Cygwin;;
    MINGW*)     machine=MinGw;;
    *)          machine="UNKNOWN:${unameOut}"
esac
echo "environment: ${machine}"

if [ "${machine}" = "Mac" ]
  then
    OUTPUT=$(mktemp -d)
  else
     OUTPUT="/tmp/res"
fi
echo "output temp path: {OUTPUT}"

./gradlew ktlint lintDebug --daemon > ${OUTPUT}

EXIT_CODE=$?
if [ ${EXIT_CODE} -ne 0 ]; then
    cat ${OUTPUT}
    rm ${OUTPUT}
    echo "Pre Commit Checks Failed. Please fix the above issues before committing"
    exit ${EXIT_CODE}
else
    rm ${OUTPUT}
    echo "Pre Commit Checks Passed -- no problems found"
fi
