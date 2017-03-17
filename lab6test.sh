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
#"test14"
#"test15"
#"test16"
#"test17"
#"test18"
#"test19"
#"test20"
#"test21"
#"test22"

)

for i in "${tests[@]}"
do
    if [[ "$1" != "" && "$1" != "$i" ]];
    then
        continue
    fi

    FILE=lab6/test/crux/test_files/$i
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
done

exit 0