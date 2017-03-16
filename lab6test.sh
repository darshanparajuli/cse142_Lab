#!/bin/bash
FILE=lab6/test/crux/test_files/$1
echo $FILE
java -jar lab6/lab6.jar $FILE.crx
diff -a <(cat ${FILE%.crx}.in  | spim -file ${FILE%.crx}.asm | tail -n +2) ${FILE%.crx}.out
echo
