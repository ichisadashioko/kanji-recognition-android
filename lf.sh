find . -type f \
    -not -path "./app/build/*" \
    -not -path "./.git/*" \
    -not -path "./captures/*" \
    -not -path "./.idea/*" \
    -not -path "./gradle/*" \
    -not -path "./.gradle/*" \
    -print0 | xargs -0 dos2unix