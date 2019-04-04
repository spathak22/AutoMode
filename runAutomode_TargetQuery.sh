target="$1"
schema="$2"
inputIndFile="$3"
inputModeFile="$4"
outputModeFile="$5"
maxIndError="$6"
storedProcedure="$7"
manualTunedConstants="$8"

echo "-------- Extracting inclusion dependencies per query --------"
java -cp dist/Castor.jar  castor.profiling.ApproximateIndSourceToTargetDiscovery -schema="$schema" -maxerror="$maxIndError" -outfile="$inputIndFile"


echo "-------- Running Automode per query  --------"
java -cp dist/Automode.jar automode.clients.AutoModePerQueryClient  -target="$target" -schema="$schema" -inputIndFile="$inputIndFile" -inputModeFile="$inputModeFile" -outputModeFile="$outputModeFile" -storedProcedure="$storedProcedure" -manualTunedConstants="$manualTunedConstants"

echo "-------- Finished Running Automode per query  --------"


