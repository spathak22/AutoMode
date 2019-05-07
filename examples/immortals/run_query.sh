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



target="$1"
inputIndFile="$2"
inputModeFile="$3"
outputModeFile="$4"
maxIndError="$5"
storedProcedure="$6"


echo "Extracting One way inclusion dependencies .... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateIndSourceToTargetDiscovery -target="$target" -maxerror="$maxIndError" -outfile="$inputIndFile" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation" -examplesRelationSuffix="$examplesRelationSuffix"

echo "Running Automode per query ... ""$target"
java -cp ../../dist/Automode.jar automode.clients.AutoModePerQueryClient  -target="$target" -inputIndFile="$inputIndFile" -inputModeFile="$inputModeFile" -outputModeFile="$outputModeFile" -storedProcedure="$storedProcedure" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation"