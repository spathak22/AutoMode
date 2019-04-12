algorithm="$1"
maxIndError="$2"
inputIndFilePath="$3"
inputModeFilePath="$4"
constThresholdType="$5"
constThresholdValue="$6"
schema="$7"
manualTunedConstants="$8"
target="$9"
storedProcedure="$10"
dbServerUrl="$11"

#Setting up AutoMode
#sh setup.sh approximate 0 automode-input/setup_inds.txt automode-input/setup_modes.json pctg 18 automode-input/manual-constants automode-input/schema-immortals.json
sh setup.sh "$algorithm" "$maxIndError" "$inputIndFilePath" "$inputModeFilePath" "$constThresholdType" "$constThresholdValue" "$manualTunedConstants" "$schema"


#Running Automode for all query
#sh run_query.sh query1 automode-input/schema-immortals.json automode-input/query1_inds.txt automode-input/setup_modes.json out/dataModel_query1.json 0 CastorProcedure_query1
sh run_query.sh query1 "$schema" automode-input/query1_inds.txt "$inputModeFilePath" out/dataModel_query1.json "$maxIndError" CastorProcedure_query1
sh run_query.sh query2 "$schema" automode-input/query2_inds.txt "$inputModeFilePath" out/dataModel_query2.json "$maxIndError" CastorProcedure_query2
sh run_query.sh query3 "$schema" automode-input/query3_inds.txt "$inputModeFilePath" out/dataModel_query3.json "$maxIndError" CastorProcedure_query3
sh run_query.sh query4 "$schema" automode-input/query4_inds.txt "$inputModeFilePath" out/dataModel_query4.json "$maxIndError" CastorProcedure_query4
sh run_query.sh query5 "$schema" automode-input/query5_inds.txt "$inputModeFilePath" out/dataModel_query5.json "$maxIndError" CastorProcedure_query5
sh run_query.sh query6 "$schema" automode-input/query6_inds.txt "$inputModeFilePath" out/dataModel_query6.json "$maxIndError" CastorProcedure_query6
sh run_query.sh query7 "$schema" automode-input/query7_inds.txt "$inputModeFilePath" out/dataModel_query7.json "$maxIndError" CastorProcedure_query7
sh run_query.sh query8 "$schema" automode-input/query8_inds.txt "$inputModeFilePath" out/dataModel_query8.json "$maxIndError" CastorProcedure_query8
sh run_query.sh query9 "$schema" automode-input/query9_inds.txt "$inputModeFilePath" out/dataModel_query9.json "$maxIndError" CastorProcedure_query9
