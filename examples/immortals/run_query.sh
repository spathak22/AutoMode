target="$1"
schema="$2"
inputIndFile="$3"
inputModeFile="$4"
outputModeFile="$5"
maxIndError="$6"
storedProcedure="$7"


echo "Extracting One way inclusion dependencies .... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateIndSourceToTargetDiscovery -target="$target" -schema="$schema" -maxerror="$maxIndError" -outfile="$inputIndFile"

echo "Running Automode per query ... ""$target"
java -cp ../../dist/Automode.jar automode.clients.AutoModePerQueryClient  -schema="$schema" -target="$target" -inputIndFile="$inputIndFile" -inputModeFile="$inputModeFile" -outputModeFile="$outputModeFile" -storedProcedure="$storedProcedure"