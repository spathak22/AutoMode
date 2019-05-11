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



inputIndFile="$1"
inputModeFile="$2"
outputModeFile="$3"
maxIndError="$4"
storedProcedure="$5"
dbUrl="$6"
port="$7"


echo "Extracting One way inclusion dependencies .... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateIndSourceToTargetDiscovery -target="$target" -maxerror="$maxIndError" -outfile="$inputIndFile" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation"

echo "Running Automode per query ... ""$target"
java -cp ../../dist/Automode.jar automode.clients.AutoModePerQueryClient  -target="$target" -inputIndFile="$inputIndFile" -inputModeFile="$inputModeFile" -outputModeFile="$outputModeFile" -storedProcedure="$storedProcedure" -examplesFile="$examplesFile" -examplesRelation="$examplesRelation" -dbUrl="$dbUrl" -port="$port"