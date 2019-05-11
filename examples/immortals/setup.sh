#!/bin/bash

while [ -n "$1" ]; do # while loop starts

    case "$1" in

    -target)
        target="$2"

        echo "-target option passed, with value $target"

        shift
        ;;

    --)
        shift # The double dash makes them parameters

        break
        ;;

    *) echo "Option $1 not recognized" ;;

    esac

    shift

done



algorithm="$1"
maxIndError="$2"
inputIndFilePath="$3"
outModeFilePath="$4"
constThresholdType="$5"
constThresholdValue="$6"
manualTunedConstants="$7"
storedProcedure="$8"
dbUrl="$9"
port="${10}"


echo "-------- Setting up AutoMode -------- "

echo "Extracting inclusion dependencies ... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateINDDiscovery -maxerror="$maxIndError" -outfile="$inputIndFilePath" -target="$target"

echo "Running Automode ... "
java -cp ../../dist/Automode.jar automode.clients.AutoModeSetupClient  -algorithm="$algorithm"  -inputIndFile="$inputIndFilePath" -outputModeFile="$outModeFilePath" -threshold="$constThresholdValue" -thresholdType="$constThresholdType" -manualTunedConstants="$manualTunedConstants" -dbUrl="$dbUrl" -port="$port" -storedProcedure="$storedProcedure" -target="$target"


echo "-------- Finished setting up Automode -------- "
