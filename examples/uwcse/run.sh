#!/bin/bash

while [ -n "$1" ]; do # while loop starts

    case "$1" in

    -examplesFile)
        examplesFile="$2"

        echo "-examplesFile option passed, with value $examplesFile"

        shift
        ;;

    -examplesRelation)
        examplesRelation="$2"

        echo "-examplesRelation option passed, with value $examplesRelation"

        shift
        ;;

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
storedProcedure="$7"
dbUrl="$8"
port="$9"
manualTunedConstants="${10}"

echo "-------- Setting up Automode ---------"
echo "Extracting inclusion dependencies ... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateINDDiscovery -maxerror="$maxIndError" -outfile="$inputIndFilePath" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation" -target="$target"

echo "Running Automode for ... " "$target"
java -cp ../../dist/Automode.jar automode.clients.AutoModeSetupClient -target="$target"  -storedProcedure="$storedProcedure"  -algorithm="$algorithm"  -inputIndFile="$inputIndFilePath" -outputModeFile="$outModeFilePath" -threshold="$constThresholdValue" -thresholdType="$constThresholdType" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation" -dbUrl="$dbUrl" -port="$port" -manualTunedConstants="$manualTunedConstants"

echo "-------- Finished setting up Automode -------- "
