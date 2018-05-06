#!/bin/sh
work_dir=$PWD
ml_mat=$work_dir/en-us-adapt/mllr_matrix
work_time=`date "+%Y%m%d_%H%M%S"`
log_file=$work_dir/$work_time.txt
MAPfileids=arctic32.fileids
MAPtrans=arctic32.transcription

#Prepare models to start with.
cd $work_dir
rm -rf en-us
rm -rf en-us-adapt
cp -r original_models/* .

cd $work_dir/traindata

for dirlist in `ls -l | awk '$1 ~ /d/ {print $9 }' `
do
	echo ">>>> START" $dirlist >> $log_file
	if [ $dirlist == "etc" ] || [ $dirlist == "logdir" ]; then
	  	echo "SKIP " $dirlist >> $log_file
	  	continue
	fi
	cd $dirlist
	cd ./English

	for dirlist2 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
	do
		echo Before enter 	`pwd` completed>> $log_file
		echo Before enter 1 $dirlist2 completed>> $log_file
		cd $dirlist2
		sphinx_fe -argfile $work_dir/en-us/feat.params -samprate 16000 -c $MAPfileids -di . -do . -ei wav -eo mfc -mswav yes

		$work_dir/bw \
			-hmmdir $work_dir/en-us-adapt \
			-moddeffn $work_dir/en-us-adapt/mdef.txt \
			-ts2cbfn .ptm. \
			-feat 1s_c_d_dd \
			-svspec 0-12/13-25/26-38 \
			-cmn current \
			-agc none \
			-dictfn $work_dir/1767.dic \
			-ctlfn $MAPfileids \
			-lsnfn $MAPtrans \
			-accumdir .

		$work_dir/map_adapt \
		    -moddeffn $work_dir/en-us-adapt/mdef.txt \
		    -ts2cbfn .ptm. \
		    -meanfn $work_dir/en-us-adapt/means \
		    -varfn $work_dir/en-us-adapt/variances \
		    -mixwfn $work_dir/en-us-adapt/mixture_weights \
		    -tmatfn $work_dir/en-us-adapt/transition_matrices \
		    -accumdir . \
		    -mapmeanfn $work_dir/en-us-adapt/means \
		    -mapvarfn $work_dir/en-us-adapt/variances \
		    -mapmixwfn $work_dir/en-us-adapt/mixture_weights \
		    -maptmatfn $work_dir/en-us-adapt/transition_matrices

		echo Dir `pwd` completed>> $log_file
		echo Dir1 $dirlist2 completed>> $log_file
		cd ..
	done

	cd ..
	cd ..
	echo ">>>> FINISH" $dirlist >> $log_file
done
echo " " >> $log_file
echo "MAP complete" >> $log_file
echo " " >> $log_file
cd $work_dir/../model_backup

mkdir $work_time
cd $work_time
cp $work_dir/en-us-adapt/* .
mv $log_file .

# rm $work_dir/en-us-adapt/*
# cp $work_dir/en-us/* $work_dir/en-us-adapt
