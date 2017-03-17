#!/bin/bash
FILE=lab6/test/crux/test_files/$1
echo ${FILE}
java -jar lab6/lab6.jar ${FILE}.crx
result=$(diff -a <(cat ${FILE%.crx}.in  | spim -file ${FILE%.crx}.asm | tail -n +2) ${FILE%.crx}.out)
if [[ -z ${result} ]]
then
    echo passed
else
    echo failed
    while read -r line; do
        echo "  $line"
    done <<< "${result}"
fi
exit 0