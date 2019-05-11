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
inputModeFilePath="$4"
constThresholdType="$5"
constThresholdValue="$6"
manualTunedConstants="$7"

#Setting up AutoMode
sh setup.sh -target "$target" -- "$algorithm" "$maxIndError" "$inputIndFilePath" "$inputModeFilePath" "$constThresholdType" "$constThresholdValue" "$manualTunedConstants"

#Running Automode for all query

sh run_query.sh -examplesRelation query1_all_pos -target "$target" --    automode-input/query1_inds.txt "$inputModeFilePath" out/dataModel_query1.json "$maxIndError" CastorProcedure_query1
sh run_query.sh -examplesRelation query2_all_pos -target "$target" --    automode-input/query2_inds.txt "$inputModeFilePath" out/dataModel_query2.json "$maxIndError" CastorProcedure_query2
sh run_query.sh -examplesRelation query3_all_pos -target "$target" --    automode-input/query3_inds.txt "$inputModeFilePath" out/dataModel_query3.json "$maxIndError" CastorProcedure_query3
sh run_query.sh -examplesRelation query4_all_pos -target "$target" --    automode-input/query4_inds.txt "$inputModeFilePath" out/dataModel_query4.json "$maxIndError" CastorProcedure_query4
sh run_query.sh -examplesRelation query5_all_pos -target "$target" --    automode-input/query5_inds.txt "$inputModeFilePath" out/dataModel_query5.json "$maxIndError" CastorProcedure_query5
sh run_query.sh -examplesRelation query6_all_pos -target "$target" --    automode-input/query6_inds.txt "$inputModeFilePath" out/dataModel_query6.json "$maxIndError" CastorProcedure_query6
sh run_query.sh -examplesRelation query7_all_pos -target "$target" --    automode-input/query7_inds.txt "$inputModeFilePath" out/dataModel_query7.json "$maxIndError" CastorProcedure_query7
sh run_query.sh -examplesRelation query8_all_pos -target "$target" --    automode-input/query8_inds.txt "$inputModeFilePath" out/dataModel_query8.json "$maxIndError" CastorProcedure_query8
sh run_query.sh -examplesRelation query9_all_pos -target "$target" --    automode-input/query9_inds.txt "$inputModeFilePath" out/dataModel_query9.json "$maxIndError" CastorProcedure_query9
