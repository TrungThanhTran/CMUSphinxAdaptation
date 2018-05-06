#!/bin/sh

work_dir=$PWD
ml_mat=$work_dir/en-us-adapt/mllr_matrix
work_time=`date "+%Y%m%d_%H%M%S"`
log_file=$work_dir/$work_time.txt
MLLRfileids=arctic_word.fileids
MLLRtrans=arctic_word.transcription
MAPfileids=arctic32.fileids
MAPtrans=arctic32.transcription

#Prepare models to start with.
cd $work_dir
rm -rf en-us
rm -rf en-us-adapt
cp -r original_models/* .

cd $work_dir/traindata_MLLR
for dirlist in `ls -l | awk '$1 ~ /d/ {print $9 }' `
do
	cd $dirlist
	cd ./English

	for dirlist2 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
	do
		cd $dirlist2
		sphinx_fe -argfile $work_dir/en-us/feat.params -samprate 16000 -c $MLLRfileids -di . -do . -ei wav -eo mfc -mswav yes

		if [ -e $ml_mat ]; then
			#$ml_mat exist
			$work_dir/bw \
			-hmmdir $work_dir/en-us-adapt \
			-moddeffn $work_dir/en-us-adapt/mdef.txt \
			-ts2cbfn .ptm. \
			-feat 1s_c_d_dd \
			-svspec 0-12/13-25/26-38 \
			-cmn current \
			-agc none \
			-mllrmat $work_dir/en-us-adapt/mllr_matrix \
			-dictfn $work_dir/1767.dic \
			-ctlfn $MLLRfileids \
			-lsnfn $MLLRtrans \
			-accumdir .
		else
			#$ml_mat not exist
			$work_dir/bw \
			-hmmdir $work_dir/en-us-adapt \
			-moddeffn $work_dir/en-us-adapt/mdef.txt \
			-ts2cbfn .ptm. \
			-feat 1s_c_d_dd \
			-svspec 0-12/13-25/26-38 \
			-cmn current \
			-agc none \
			-dictfn $work_dir/1767.dic \
			-ctlfn $MLLRfileids \
			-lsnfn $MLLRtrans \
			-accumdir .
		fi
		$work_dir/mllr_solve \
    	-meanfn $work_dir/en-us-adapt/means \
   		-varfn $work_dir/en-us-adapt/variances \
  		-outmllrfn $work_dir/en-us-adapt/mllr_matrix \
			-accumdir .

		echo `pwd` completed >> $log_file
		cd ..
	done

	cd ..
	cd ..
done
echo " " >> $log_file
echo "MLLR complete" >> $log_file
echo " " >> $log_file

cd $work_dir/traindata

for dirlist in `ls -l | awk '$1 ~ /d/ {print $9 }' `
do
	echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
	echo ">>>> START" $dirlist >> $log_file
	if [ $dirlist == "etc" ] || [ $dirlist == "logdir" ]; then
	  	echo "SKIP " $dirlist >> $log_file
	  	continue
	fi
	cd $dirlist
	cd ./English

	for dirlist2 in `ls -l | awk '$1 ~ /d/ {print $9 }' `
	do
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
			-mllrmat $work_dir/en-us-adapt/mllr_matrix \
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

		echo "`pwd`/`ls *.wav`" complete >> $log_file
		cd ..

	done

	cd ..
	cd ..
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
