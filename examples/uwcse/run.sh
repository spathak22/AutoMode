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

    -examplesRelationSuffix)
        examplesRelationSuffix="$2"

        echo "-examplesRelationSuffix option passed, with value $examplesRelationSuffix"

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
target="$7"
storedProcedure="$8"
dbServerUrl="$9"
schema="$10"
manualTunedConstants="$11"

echo "-------- Setting up Automode ---------"
echo "Extracting inclusion dependencies ... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateINDDiscovery -maxerror="$maxIndError" -outfile="$inputIndFilePath" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation" -examplesRelationSuffix="$examplesRelationSuffix"

echo "Running Automode for ... " "$target"
java -cp ../../dist/Automode.jar automode.clients.AutoModeSetupClient -target="$target"  -storedProcedure="$storedProcedure"  -algorithm="$algorithm"  -inputIndFile="$inputIndFilePath" -outputModeFile="$outModeFilePath" -threshold="$constThresholdValue" -thresholdType="$constThresholdType" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation"

echo "-------- Finished setting up Automode -------- "
