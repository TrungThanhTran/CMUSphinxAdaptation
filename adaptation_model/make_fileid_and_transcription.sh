#!/bin/sh

work_dir=~/AMBuilding_workspace/AcousticModel_Build/en-prdcv/Acoustic
train_fileid_path=$work_dir/etc/en-prdcv_train.fileids
train_transcript_path=$work_dir/etc/en-prdcv_train.transcription
test_fileid_path=$work_dir/etc/en-prdcv_test.fileids
test_transcript_path=$work_dir/etc/en-prdcv_test.transcription
list_path=$work_dir/wordlist.txt
lang=English

cd $work_dir/wav

for dirlist0 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
do
  cd $dirlist0
  for dirlist1 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
  do
    cd $dirlist1
    for dirlist2 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
    do
      cd $dirlist2
      cd ./$lang
      for dirlist3 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
      do
        cd $dirlist3
        if [ ${dirlist0} = "train" ]; then
          echo "traindata preparing"
          echo `pwd`
        	#Getting the full path of wave file
          ls -ld $PWD/* | gawk '{print $9}' | grep -e "\.wav"  > tem.fileid
        	#Removieng the previous part of full path
          sed -i -e "s/\/home\/koki\/AMBuilding_workspace\/AcousticModel_Build\/en-prdcv\/Acoustic\/wav\///g" tem.fileid
        	#Removing the extantion
          sed -i -e "s/\.wav//g" tem.fileid
          cat tem.fileid >> $train_fileid_path
        	#picking up only file name
          awk -F '/' '{print $6}' tem.fileid > tem.txt
        	#Enclosing the name in "(", ")"
          sed 's/^/\(/g' tem.txt | sed 's/$/\)/g' > tem1.txt
          paste -d' ' $list_path tem1.txt >> $train_transcript_path
          rm tem.fileid
          rm tem.txt
          rm tem1.txt
        else
          echo "testdata preparing"
          echo `pwd`
          ls -ld $PWD/* | gawk '{print $9}' | grep -e "\.wav"  > tem.fileid
          sed -i -e "s/\/home\/koki\/AMBuilding_workspace\/AcousticModel_Build\/en-prdcv\/Acoustic\/wav\///g" tem.fileid
          sed -i -e "s/\.wav//g" tem.fileid
          cat tem.fileid >> $test_fileid_path
          awk -F '/' '{print $6}' tem.fileid > tem.txt
          sed 's/^/\(/g' tem.txt | sed 's/$/\)/g' > tem1.txt
          paste -d' ' $list_path tem1.txt >> $test_transcript_path
          rm tem.fileid
          rm tem.txt
          rm tem1.txt
        fi
        cd ..
      done

      cd ..
      cd ..
    done
    cd ..
  done
  cd ..
done
