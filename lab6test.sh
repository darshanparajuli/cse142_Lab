#!/bin/bash

BUILD_DIR=lab6/build
TESTS_DIR=lab6/test/crux/test_files
TESTS=(
"test01"
"test02"
"test03"
"test04"
"test05"
"test06"
"test07"
"test08"
"test09"
"test10"
"test11"
"test12"
"test13"
"test14"
"test15"
"test16"
"test17"
"test18"
"test19"
"test20"
"test21"
"test22"
)

echo "> Building"

rm -rf ${BUILD_DIR}
mkdir -p ${BUILD_DIR}
javac $(find ./lab6/src -name "*.java") -d ${BUILD_DIR}

ret_val=0

echo "> Running tests in "${TESTS_DIR}
for i in "${TESTS[@]}"
do
    if [[ "$1" != "" && "$1" != "$i" ]];
    then
        continue
    fi

    FILE=${TESTS_DIR}/$i
    java -classpath ${BUILD_DIR} crux.Compiler ${FILE}.crx
    result=$(diff -a <(cat ${FILE%.crx}.in  | spim -file ${FILE%.crx}.asm | tail -n +2) ${FILE%.crx}.out)
    test_name=${FILE##*/}
    if [[ -z ${result} ]]
    then
        echo ${test_name}: PASS
    else
        ret_val=1
        echo ${test_name}: FAIL
        while read -r line; do
            echo "  $line"
        done <<< "${result}"
    fi
done

exit ${ret_val}