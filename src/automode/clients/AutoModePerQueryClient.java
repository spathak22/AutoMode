package automode.clients;

import automode.algorithms.AutoModePerQuery;
import automode.util.Commons;
import automode.util.JsonUtil;
import castor.settings.DataModel;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FilenameFilter;

public class AutoModePerQueryClient {

    //protected String dbServerURL = "localhost:21212";

    @Option(name = "-inputIndFile", usage = "Input IND file path", required = true)
    public String inputIndFile = null;

    @Option(name = "-schema", usage = "Input schema Json file", required = true)
    public String schema = null;

    @Option(name = "-target", usage = "Target query to be learned", required = true)
    public String target = null;

    @Option(name = "-inputModeFile", usage = "Input mode file path", required = true)
    public String inputModeFile = null;

    @Option(name = "-outputModeFile", usage = "Output mode file path", required = true)
    public String outputModeFile = null;

    @Option(name = "-dirInput", usage = "Run on a dir in input", required = false)
    public boolean dirInput = false;

    @Option(name = "-dirPath", usage = "Input directory path", required = false)
    public String dirPath = null;

    @Option(name = "-fileInput", usage = "Run on individual file in input", required = false)
    public boolean fileInput = true;

    @Option(name = "-storedProcedure", usage = "Voltdb procedure", required = false)
    public String storedProcedure = null;

    @Option(name = "-manualTunedConstants", usage = "File to read manual tuned constants", required = false)
    public String manualTunedConstants = null;

    final static Logger logger = Logger.getLogger(AutoModePerQueryClient.class);

    public static void main(String[] args) {
        logger.debug("Inside algorithms");
        AutoModePerQueryClient am = new AutoModePerQueryClient();
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
                //For immortal dataset update the stored prodedure name wrt queries
                String [] query  = dir.split("_");
				target = query[1]+"_all";
				storedProcedure = "CastorProcedure_"+query[1]+"_all";

                callAutoModeGenerators();
            }
        }
    }

    /**
     * Call the mode generation algorithms
     */
    public void callAutoModeGenerators() {
        AutoModePerQuery autoMode = new AutoModePerQuery();
        DataModel dataModel = autoMode.connectHeadToBodyModes(target, schema, inputModeFile, inputIndFile);
        Commons.resetUniqueVertexTypeGenerator();

        if(storedProcedure==null)
            storedProcedure = dataModel.getSpName();
        JsonUtil.writeModeToJsonFormat(null, dataModel.getModeH().toString(), dataModel.getModesBString(), storedProcedure, outputModeFile);
//        if(outputIndFile!=null)
//            JsonUtil.writeIndsToJsonFormat(dataModel.getInds(),dataModel.getDbRelations(),outputIndFile,target);    }
    }
}
