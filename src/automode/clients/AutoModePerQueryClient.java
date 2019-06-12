package automode.clients;

import automode.algorithms.AutoModePerQuery;
import automode.db.VoltDBQuery;
import automode.util.Commons;
import automode.util.Constants;
import automode.util.JsonUtil;
import castor.language.Schema;
import castor.settings.DataModel;
import castor.settings.JsonSettingsReader;
import castor.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class AutoModePerQueryClient {

    //protected String dbServerURL = "localhost:21212";

    @Option(name = "-inputIndFile", usage = "Input IND file path", required = true)
    public String inputIndFile = null;

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

    @Option(name = "-dbUrl", usage = "URL of running db instance", required = false)
    public String dbUrl = Constants.Voltdb.URL.getValue();

    @Option(name = "-port", usage = "Port number of running db instance", required = false)
    public String port = Constants.Voltdb.PORT.getValue();

    @Option(name = "-examplesRelation", usage = "Examples Relation", required = false)
    public String examplesRelation = Constants.Regex.EMPTY_STRING.getValue();

    @Option(name = "-examplesFile", usage = "Files Relation", required = false)
    public String examplesFile = Constants.Regex.EMPTY_STRING.getValue();

    final static Logger logger = Logger.getLogger(AutoModePerQueryClient.class);

    public static void main(String[] args) {
        logger.debug("Inside algorithms");
        AutoModePerQueryClient am = new AutoModePerQueryClient();
        am.runAutomode(args);
    }

    /**
     * Parse the input arguments and call the mode generation flow
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
            //Check if the exampleFile is in input
            if (!examplesFile.isEmpty()) {
                examplesRelation = FilenameUtils.getBaseName(new File(examplesFile).getName());
                logger.debug("examplesRelation : " + examplesRelation);
            }
            if(dbUrl.isEmpty()){
                dbUrl = Constants.Voltdb.URL.getValue();
            }
            if(port.isEmpty()){
                port = Constants.Voltdb.PORT.getValue();
            }

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
                String[] query = dir.split("_");
                target = query[1] + "_all";
                storedProcedure = "CastorProcedure_" + query[1] + "_all";

                callAutoModeGenerators();
            }
        }
    }

    /**
     * Call the mode generation algorithms
     */
    public void callAutoModeGenerators() {
        String url = dbUrl + ":" + port;
        Schema schemaObj = null;
        VoltDBQuery vQuery = new VoltDBQuery();
        schemaObj = vQuery.getSchema(url);
        //Obsolete reading schema from json
        //schemaObj = JsonSettingsReader.readSchema(FileUtils.convertFileToJSON(schema));

        AutoModePerQuery autoMode = new AutoModePerQuery();
        DataModel dataModel = autoMode.connectHeadToBodyModes(target, schemaObj, inputModeFile, inputIndFile);
        Commons.resetUniqueVertexTypeGenerator();

        if (storedProcedure.isEmpty())
            storedProcedure = dataModel.getSpName();

        //Remove duplicate modes
        LinkedHashSet<String> bodyModesStringSet = new LinkedHashSet<>(dataModel.getModesBString());
        List<String> bodyModesString = new ArrayList<>(bodyModesStringSet);

        if(dataModel.getModeH()!=null)
            JsonUtil.writeModeToJsonFormat(null, dataModel.getModeH().toString(), bodyModesString, storedProcedure, outputModeFile);
//        if(outputIndFile!=null)
//            JsonUtil.writeIndsToJsonFormat(dataModel.getInds(),dataModel.getDbRelations(),outputIndFile,target);    }
    }
}
