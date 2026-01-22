#!/bin/bash
set -eu

if [ "${ENV_VERBOSE}" = "true" ] ; then
  set -x
fi

python3 /app/script.py > "$(results.execution-result.path)"

EXIT_CODE="$?"
if [ "${EXIT_CODE}" != 0 ] ; then
  exit "${EXIT_CODE}"
fi

echo sm-stop-log
