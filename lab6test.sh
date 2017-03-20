#!/bin/bash

tests=(
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


build_dir=lab6/build
test_dir=lab6/test/crux/test_files

echo "> Building"

rm -rf ${build_dir}
mkdir -p ${build_dir}
javac $(find ./lab6/src -name "*.java") -d ${build_dir}

echo "> Running tests in "${test_dir}
for i in "${tests[@]}"
do
    if [[ "$1" != "" && "$1" != "$i" ]];
    then
        continue
    fi

    FILE=${test_dir}/$i
    java -classpath ${build_dir} crux.Compiler  ${FILE}.crx
    result=$(diff -a <(cat ${FILE%.crx}.in  | spim -file ${FILE%.crx}.asm | tail -n +2) ${FILE%.crx}.out)
    test_name=${FILE##*/}
    if [[ -z ${result} ]]
    then
        echo ${test_name}: PASS
    else
        echo ${test_name}: FAIL
        while read -r line; do
            echo "  $line"
        done <<< "${result}"
    fi
done

exit 0