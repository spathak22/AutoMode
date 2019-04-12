algorithm="$1"
maxIndError="$2"
inputIndFilePath="$3"
outModeFilePath="$4"
constThresholdType="$5"
constThresholdValue="$6"
manualTunedConstants="$7"
schema="$8"
target="$9"
storedProcedure="$10"
dbServerUrl="$11"

echo "-------- Setting up AutoMode -------- "

echo "Extracting inclusion dependencies ... "
java -cp ../../dist/Automode.jar  automode.profiling.ApproximateINDDiscovery -maxerror="$maxIndError" -outfile="$inputIndFilePath" -schema="$schema"

echo "Running Automode ... "
java -cp ../../dist/Automode.jar automode.clients.AutoModeSetupClient  -algorithm="$algorithm"  -inputIndFile="$inputIndFilePath" -outputModeFile="$outModeFilePath" -threshold="$constThresholdValue" -thresholdType="$constThresholdType" -manualTunedConstants="$manualTunedConstants"

echo "-------- Finished setting up Automode -------- "
