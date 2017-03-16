#!/bin/bash
cd build

for FILE in $(ls ../lab6/test/crux/test_files/*.crx)
do
    echo $FILE
    java -jar lab6.jar $FILE
    diff -a <(cat ${FILE%.crx}.in  | spim -file ${FILE%.crx}.asm) ${FILE%.crx}.out
    echo
done
