#!/bin/bash

# Copyright 2017 Juno, Inc.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

echo ":: mainframer v2.1.0"
echo ""

START_TIME="$(date +%s)"

PROJECT_DIR="$(pwd)"
PROJECT_DIR_NAME="$( basename "$PROJECT_DIR" )"
PROJECT_DIR_ON_REMOTE_MACHINE="~/mainframer/$PROJECT_DIR_NAME"

CONFIG_DIR="$PROJECT_DIR/.mainframer"
CONFIG_FILE="$CONFIG_DIR/config"
COMMON_IGNORE_FILE="$CONFIG_DIR/ignore"
LOCAL_IGNORE_FILE="$CONFIG_DIR/localignore"
REMOTE_IGNORE_FILE="$CONFIG_DIR/remoteignore"

function readConfigProperty {
	grep "^${1}=" "$CONFIG_FILE" | cut -d'=' -f2
}

REMOTE_MACHINE_CONFIG_PROPERTY="remote_machine"
LOCAL_COMPRESS_LEVEL_CONFIG_PROPERTY="local_compression_level"
REMOTE_COMPRESS_LEVEL_CONFIG_PROPERTY="remote_compression_level"

if [ ! -f "$CONFIG_FILE" ]; then
	echo "Please create and fill $CONFIG_FILE."
	exit 1
fi

REMOTE_MACHINE=$(readConfigProperty "$REMOTE_MACHINE_CONFIG_PROPERTY")
LOCAL_COMPRESS_LEVEL=$(readConfigProperty "$LOCAL_COMPRESS_LEVEL_CONFIG_PROPERTY")
REMOTE_COMPRESS_LEVEL=$(readConfigProperty "$REMOTE_COMPRESS_LEVEL_CONFIG_PROPERTY")

if [ -z "$REMOTE_MACHINE" ]; then
	echo "Please specify \"$REMOTE_MACHINE_CONFIG_PROPERTY\" in $CONFIG_FILE."
	exit 1
fi

if [ -z "$LOCAL_COMPRESS_LEVEL" ]; then
	LOCAL_COMPRESS_LEVEL=1
fi

if [ -z "$REMOTE_COMPRESS_LEVEL" ]; then
	REMOTE_COMPRESS_LEVEL=1
fi


REMOTE_COMMAND="$*"
REMOTE_COMMAND_SUCCESSFUL="false"

if [ -z "$REMOTE_COMMAND" ]; then
	echo "Please pass remote command."
	exit 1
fi

function formatTime {
	local time=$1

	local hours=$((time / 3600))
	local minutes=$(((time % 3600) / 60))
	local seconds=$((time % 60))

	if [ "$hours" -eq "1" ]; then HOURS_LABEL="hour"; else HOURS_LABEL="hours"; fi
	if [ "$minutes" -eq "1" ]; then MINUTES_LABEL="minute"; else MINUTES_LABEL="minutes"; fi
	if [ "$seconds" -eq "1" ]; then SECONDS_LABEL="second"; else SECONDS_LABEL="seconds"; fi

	(( hours > 0 )) && printf "%d $HOURS_LABEL " ${hours}
	(( minutes > 0 )) && printf "%d $MINUTES_LABEL " ${minutes}
	(( seconds >= 0 )) && printf "%d $SECONDS_LABEL" ${seconds}
}

function syncBeforeRemoteCommand {
	echo "Sync local → remote machine..."
	startTime="$(date +%s)"

	COMMAND="rsync --archive --delete --rsync-path=\"mkdir -p \"$PROJECT_DIR_ON_REMOTE_MACHINE\" && rsync\" --compress-level=$LOCAL_COMPRESS_LEVEL "

	if [ -f "$COMMON_IGNORE_FILE" ]; then
		COMMAND+="--exclude-from='$COMMON_IGNORE_FILE' "
	fi

	if [ -f "$LOCAL_IGNORE_FILE" ]; then
		COMMAND+="--exclude-from='$LOCAL_IGNORE_FILE' "
	fi

	COMMAND+="--rsh ssh ./ $REMOTE_MACHINE:'$PROJECT_DIR_ON_REMOTE_MACHINE'"

	eval "$COMMAND"

	endTime="$(date +%s)"
	echo "Sync done: took $(formatTime $((endTime-startTime)))."
	echo ""
}

function executeRemoteCommand {
	echo "Executing command on remote machine…"
	echo ""
	startTime="$(date +%s)"

	set +e
	if ssh "$REMOTE_MACHINE" "echo 'set -e && cd '$PROJECT_DIR_ON_REMOTE_MACHINE' && echo \"$REMOTE_COMMAND\" && echo "" && $REMOTE_COMMAND' | bash" ; then
		REMOTE_COMMAND_SUCCESSFUL="true"
	fi
	set -e

	endTime="$(date +%s)"
	echo ""

	duration="$((endTime-startTime))"

	if [ "$REMOTE_COMMAND_SUCCESSFUL" == "true" ]; then
		echo "Execution done: took $(formatTime $duration)."
	else
		echo "Execution failed: took $(formatTime $duration)."
	fi

	echo ""
}

function syncAfterRemoteCommand {
	echo "Sync remote → local machine…"
	startTime="$(date +%s)"

	COMMAND="rsync --archive --delete --compress-level=$REMOTE_COMPRESS_LEVEL "

	if [ -f "$COMMON_IGNORE_FILE" ]; then
		COMMAND+="--exclude-from='$COMMON_IGNORE_FILE' "
	fi

	if [ -f "$REMOTE_IGNORE_FILE" ]; then
		COMMAND+="--exclude-from='$REMOTE_IGNORE_FILE' "
	fi

	COMMAND+="--rsh ssh $REMOTE_MACHINE:'$PROJECT_DIR_ON_REMOTE_MACHINE'/ ./"
	eval "$COMMAND"

	endTime="$(date +%s)"
	echo "Sync done: took $(formatTime $((endTime-startTime)))."
}

pushd "$PROJECT_DIR" > /dev/null

syncBeforeRemoteCommand
executeRemoteCommand
syncAfterRemoteCommand

popd > /dev/null

FINISH_TIME="$(date +%s)"
echo ""

DURATION="$((FINISH_TIME-START_TIME))"

if [ "$REMOTE_COMMAND_SUCCESSFUL" == "true" ]; then
	echo "Success: took $(formatTime $DURATION)."
else
	echo "Failure: took $(formatTime $DURATION)."
	exit 1
fi
