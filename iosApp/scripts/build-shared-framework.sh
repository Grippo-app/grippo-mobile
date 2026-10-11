#!/bin/sh
set -eu

# Make the Kotlin build a prerequisite of Swift compilation/linking, even in an IDE build.
cd "${SRCROOT}/.."
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME=$(/usr/libexec/java_home)
    export JAVA_HOME
fi
./gradlew :shared:embedAndSignAppleFrameworkForXcode
