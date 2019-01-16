#!/bin/bash
#
# ./sysmanager.sh [start|stop|clear]
#
# Copyright (c) 2013-2014, Imperial College London
# All rights reserved.
#
# Distributed Algorithms, CO347
#
PIDDIR="`pwd`/pids"

VERBOSE=true

# If true, stdout is redirected to
# log files, one per process.
LOG=true

USAGE="./sysmanager.sh [start|stop|clear] [class] [#instances] [topology]"

daemonize () {
	name=$1
	# The name of a process occurs twice; 
	# the first occurence is used for the
	# .pid file.
	shift 1
	(
	[[ -t 0 ]] && exec 0</dev/null
	if $LOG; then
		[[ -t 1 ]] && exec 1>"`pwd`"/${name}.out
	fi
	# Always redirect stderr to a file.
	[[ -t 2 ]] && exec 2>"`pwd`"/${name}.err
	
	# close non-standard file descriptors
	eval exec {3..255}\>\&-
	trap '' 1 2 # ignore HUP INT in child process
	exec "$@"
	) &
	pid=$!
	disown -h $pid
	$VERBOSE && echo "[DBG] ${name}'s pid is ${pid}"
	echo $pid > "${PIDDIR}"/${name}.pid
	return 0
}

check () {
	name=$1
	$VERBOSE && echo "[DBG] checking ${name}"
	# Check if process $name is running
	[ -s "${PIDDIR}"/$name.pid ] && (
		$VERBOSE && echo "[DBG] ${name}.pid found"
		pid=`cat "${PIDDIR}"/$name.pid`
		ps -p $pid &>/dev/null
		return $?
	) 
}

start () {
	$VERBOSE && echo "[DBG] start ${N} instances of class ${P}"
	i=1
	while [ $i -le $N ]; do
		name="P${i}"
		# You can append arguments args[3], args[4],
		# and so on after ${N}.
		daemonize ${name} java ${P} ${name} ${i} ${N} $@
		let i++
	done
}

clear () {
	$VERBOSE && echo "[DBG] clear"
	[ -d "${PIDDIR}" ] && rm -f "${PIDDIR}"/*.pid
	rm -rf "${PIDDIR}"
	# Delete empty *.err files
	ls *.err &>/dev/null
	if [ $? -eq 0 ]; then
		files=`ls *.err`
		for f in $files; do
			[ ! -s $f ] && rm -f $f
		done
	fi
}

stop () {
	$VERBOSE && echo "[DBG] stop"
	[ ! -d "${PIDDIR}" ] && return 0
	ls "${PIDDIR}"/*.pid &>/dev/null
	if [ $? -eq 0 ]; then
		for f in "${PIDDIR}"/*.pid; do
			pid=`cat "$f"`
			$VERBOSE && echo "[DBG] pid is $pid"
			kill -9 $pid &>/dev/null
			rm -f "$f"
		done
	fi
	clear # delete $PIDDIR.
}

#
# main ($1) ($2) ($3) (...)
#
if [ $# -lt 1 ]; then
	echo $USAGE && exit 1
else
	if [ $1 == "start" ]; then
		
		if [ $# -lt 4 ]; then # Check number of arguments.
			echo $USAGE
			exit 1
		fi
		
		if [ ! -f "$2.class" ]; then # Check program name.
			echo "error: $2.class not found"
			exit 1
		fi
		# Check number of processes.
		[ $3 -eq $3 ] >/dev/null 2>&1
		if [ $? -eq 1 ]; then
			echo "error: invalid argument ($3)"
			exit 1
		fi
		
		if [ $3 -le 0 ]; then
			echo "error: invalid argument ($3)"
			exit 1
		fi
		
		if [ ! -f "$4" ]; then # Check if topology file exists.
			echo $USAGE
			exit
		fi
		
	elif [ $# -ne 1 ]; then
			echo $USAGE
			exit 1
	fi
fi

C=$1 # start, stop, or clear
P=$2 # class name
N=$3 # #instances
F=$4 # network filename
shift 4
# Arguments 4, 5, 6, ... are passed to the process when it starts.

case ${C} in
	"start")
	
	[ -d "${PIDDIR}" ] || mkdir -p "${PIDDIR}"
	
	# To begin with, start Registrar
	check "P0"
	if [ $? -eq 0 ]; then
		echo "error: Registrar already running"
		exit 1
	fi
	daemonize "P0" java Registrar $N $F
	sleep 1
	
	start $@ ;;
	"stop" )
	stop ;;
	"clear")
	clear;;
	*)
	echo $USAGE
	exit 1
esac

exit 0

