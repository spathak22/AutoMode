package automode.clients;

import automode.algorithms.AutoMode;
import automode.algorithms.AutoModeApproximate;
import automode.algorithms.AutoModeExact;
import automode.helper.IndHelper;
import automode.util.Commons;
import automode.util.Constants;
import automode.util.JsonUtil;
import castor.settings.DataModel;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FilenameFilter;

public class AutoModeSetupClient {

    //protected String dbServerURL = "localhost:21212";
    @Option(name = "-dbUrl", usage = "URL of running db instance", required = false)
    public String dbUrl = Constants.Voltdb.URL.getValue();

    @Option(name = "-target", usage = "Target schema", required = false)
    public String target = null;

    @Option(name = "-storedProcedure", usage = "Voltdb procedure", required = false)
    public String storedProcedure = null;

    @Option(name = "-algorithm", usage = "Exact run", required = true)
    public String algorithm = null;

    @Option(name = "-approx", usage = "Approx run")
    public boolean approx = false;

    @Option(name = "-dirInput", usage = "Run on a dir in input", required = false)
    public boolean dirInput = false;

    @Option(name = "-dirPath", usage = "Input directory path", required = false)
    public String dirPath = null;

    @Option(name = "-fileInput", usage = "Run on individual file in input", required = false)
    public boolean fileInput = false;

    @Option(name = "-inputIndFile", usage = "Input IND file path", required = true)
    public String inputIndFile = null;

    @Option(name = "-outputModeFile", usage = "Output Json mode file path", required = true)
    public String outputModeFile = null;

    @Option(name = "-outputIndFile", usage = "Output Json IND file path", required = false)
    public String outputIndFile = null;

    @Option(name = "-outputLog", usage = "Output Automode log path", required = false)
    public String outputLog = null;

    @Option(name = "-threshold", usage = "Threshold value", required = true)
    public int threshold = 0;

    @Option(name = "-thresholdType", usage = "Threshold type abs or pctg", required = true)
    public String thresholdType = null;

    final static Logger logger = Logger.getLogger(AutoModeSetupClient.class);

    public static void main(String[] args) {
        logger.debug("Inside algorithms");
        AutoModeSetupClient am = new AutoModeSetupClient();
        am.runAutomode(args);
    }


    /**
     *  Parse the input arguments and call the mode generation flow
     */
    public void runAutomode(String[] args) {
        // Parse the arguments
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            logger.error(e.getMessage());
            parser.printUsage(System.out);
            return;
        }

//        LogUtil.clearLog(outputLog);

        logger.debug("Inside run algorithm is : " + algorithm);
        logger.debug("Inside run target is : " + target + ", sp is " + storedProcedure);

        if (fileInput) {
            callAutoModeGenerators();
        } else if (dirInput) {
            File file = new File(dirPath);
            String[] directories = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory();
                }
            });

            for (String dir : directories) {
                //Dir path is outside directory, "dir" is all the directory inside the parent directory and inputIndFile,outputModeFile is the structure inside "dir" like "/castor-input/inds.txt"
                inputIndFile = dirPath + dir + inputIndFile;
                outputModeFile = dirPath + dir + outputModeFile;
                /* For immortal dataset update the stored prodedure name wrt queries
                String [] query  = dir.split("_");
				target = query[1]+"_all";
				storedProcedure = "CastorProcedure_"+query[1]+"_all";
				*/
                callAutoModeGenerators();
            }
        }
    }

    /**
     * Call the mode generation algorithms
     */
    public void callAutoModeGenerators() {
        AutoMode autoMode = null;
        IndHelper indHelper = new IndHelper();
        if (algorithm.equals("exact")) {
            logger.debug("Inside exact");
            autoMode = new AutoModeExact();
        } else {
            if (algorithm.equals("approximate")) {
                logger.debug("Inside approximate");
                autoMode = new AutoModeApproximate();
            }
        }

        DataModel dataModel = autoMode.runModeBuilder(indHelper, threshold, thresholdType, target, storedProcedure, dbUrl, inputIndFile, outputModeFile, outputIndFile);
        Commons.resetUniqueVertexTypeGenerator();

        //Write Modes and INDS Output files
        String headMode = null;
        if (target != null) {
            headMode = dataModel.getModeH().toString();
        }
        JsonUtil.writeModeToJsonFormat(null, headMode, dataModel.getModesBString(), storedProcedure, outputModeFile);
        if (outputIndFile != null)
            JsonUtil.writeIndsToJsonFormat(indHelper.getInds(), indHelper.getDbRelations(), outputIndFile, target);
        logger.debug("-------- Finished setting up Automode ---------");
    }

}
