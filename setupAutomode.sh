algorithm="$1"
schema="$2"
maxIndError="$3"
outIndFilePath="$4"
outModeFilePath="$5"
dbServerUrl="$6"
constThresholdType="$7"
constThresholdValue="$8"
manualTunedConstants="$9"

echo "-------- Extracting inclusion dependencies -------- "
java -cp dist/Castor.jar  castor.profiling.ApproximateINDDiscovery -schema="$schema" -maxerror="$maxIndError" -outfile="$outIndFilePath"

echo "-------- Setting up AutoMode -------- "
java -cp dist/Automode.jar automode.clients.AutoModeSetupClient -algorithm="$algorithm" -dbUrl="$dbServerUrl"  -approx -fileInput -inputIndFile="$outIndFilePath" -outputModeFile="$outModeFilePath" -threshold="$constThresholdValue" -thresholdType="$constThresholdType"

echo "-------- Finished setting up Automode -------- "
