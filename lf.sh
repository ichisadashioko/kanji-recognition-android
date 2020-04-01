#!/bin/bash
find . -type f \
    -not -path "./app/build/*" \
    -not -path "./.git/*" \
    -not -path "./captures/*" \
    -not -path "./.idea/*" \
    -not -path "./gradle/*" \
    -not -path "./.gradle/*" \
    -not -path "./.project" \
    -not -path "./.settings/*" \
    -not -path "./app/app.iml" \
    -not -path "./app/.classpath" \
    -not -path "./app/.settings/*" \
    -not -path "./app/.project" \
    -print0 | xargs -0 dos2unix