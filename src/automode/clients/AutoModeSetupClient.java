package automode.clients;

import automode.algorithms.AutoMode;
import automode.algorithms.AutoModeApproximate;
import automode.algorithms.AutoModeExact;
import automode.helper.IndHelper;
import automode.util.Commons;
import automode.util.Constants;
import automode.util.JsonUtil;
import castor.language.*;
import castor.settings.DataModel;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AutoModeSetupClient {

    //protected String dbServerURL = "localhost:21212";
    @Option(name = "-dbUrl", usage = "URL of running db instance", required = false)
    public String dbUrl = Constants.Voltdb.URL.getValue();

    //protected String dbServerURL = "localhost:21212";
    @Option(name = "-port", usage = "Port number of running db instance", required = false)
    public String port = Constants.Voltdb.PORT.getValue();

    @Option(name = "-target", usage = "Target schema", required = false)
    public String target = null;

    @Option(name = "-examplesRelation", usage = "Examples Relation", required = false)
    public String examplesRelation = Constants.Regex.EMPTY_STRING.getValue();

    @Option(name = "-examplesFile", usage = "Files Relation", required = false)
    public String examplesFile = Constants.Regex.EMPTY_STRING.getValue();

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
    public boolean fileInput = true;

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

    @Option(name = "-manualTunedConstants", usage = "File to read manual tuned constants", required = false)
    public String manualTunedConstants = null;

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
            //Check if the exampleFile is in input
            if (!examplesFile.isEmpty()) {
                examplesRelation = FilenameUtils.getBaseName(new File(examplesFile).getName());
                logger.debug("examplesRelation : "+examplesRelation);
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
        String url = dbUrl + ":" + port;
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

        DataModel dataModel = autoMode.runModeBuilder(examplesFile, examplesRelation, indHelper, threshold, thresholdType, target, storedProcedure, url, inputIndFile, outputModeFile, outputIndFile);
        Commons.resetUniqueVertexTypeGenerator();

        //If manualTunedModes is not null then remove the modes which has unwanted constants
        logger.debug(" manualTunedConstants value : "+manualTunedConstants);
        if(!manualTunedConstants.isEmpty()){
            dataModel.setModesB(this.manualTunedTheConstants(dataModel.getModesB(), manualTunedConstants, indHelper.getDbRelations()));
        }

        //Write Modes and INDS Output files
        String headMode = null;
        if (!examplesRelation.isEmpty()) {
            headMode = dataModel.getModeH().toString();
        }

        //If storedProcedure is empty
        if(storedProcedure.isEmpty())
            storedProcedure=null;

        JsonUtil.writeModeToJsonFormat(null, headMode, dataModel.getModesBString(), storedProcedure, outputModeFile);
        if (outputIndFile != null)
            JsonUtil.writeIndsToJsonFormat(indHelper.getInds(), indHelper.getDbRelations(), outputIndFile, target);
        logger.debug("-------- Finished setting up Automode ---------");
    }


    /**
     * Remove modes having unwanted constants from modes body
     */
    public List<Mode> manualTunedTheConstants(List<Mode> modesB, String constantsPath, Map<String, List<String>> relations){
        //Create data structure for input constantsDefinition file
        Map<String, Set<Integer>> constantMap = new HashMap<>();
        Stream<String> stream = null;
        try {
            logger.info("Reading Constants deffinition file...");
            stream = Files.lines(Paths.get(constantsPath));
            {
                stream.filter(s -> !s.isEmpty()).forEach(constantsDef -> processConstants(constantsDef, constantMap, relations));
            }
        } catch (IOException e) {
            logger.error("Error while processing Inds file");
            e.printStackTrace();
        }
        stream.close();

        //Loop through modes and check if it has unwanted constants
        List<Mode> modesTuned = new ArrayList<>();

        for(Mode mode: modesB){
            boolean unwantedModeFlag = false;
            String predicateName = mode.getPredicateName();
            if(constantMap.containsKey(predicateName)) {
                int argumentIndex = 0;
                for (Argument argument : mode.getArguments()) {
                    if(argument.getIdentifierType()== IdentifierType.CONSTANT){
                        if(!constantMap.get(predicateName).contains(argumentIndex)){
                            unwantedModeFlag = true;
                            break;
                        }
                    }
                    argumentIndex++;
                }
            }

            if(!unwantedModeFlag)
                modesTuned.add(mode);
        }
        return modesTuned;
    }



    /**
     * Populate constants Map to store : Relation: {Attributeindex1, Attributeindex2}
     */
    public void processConstants(String constantDef, Map<String, Set<Integer>> constantsMap, Map<String, List<String>> relations) {
        String formattedConstantDef = constantDef.trim().replaceAll("\\s", "");
        String[] constantDefLine = formattedConstantDef.split(":");
        String relation = constantDefLine[0];
        String[] attributes = constantDefLine[1].split(Pattern.quote(Constants.Regex.COMMA.getValue()));
        Set<Integer> attributesSet = new HashSet<>();
        for(String atribute: attributes){
            attributesSet.add(getAttributePositionInRelation(relations, relation, atribute));
        }
        constantsMap.put(relation,attributesSet);
    }


    /**
     * Get position of an attribute in relation
     */
    public int getAttributePositionInRelation(Map<String, List<String>>  relations, String relation, String attribute) {
        List<String> attributes = relations.get(relation);
        int attributeIndex = -1;
        for (int i = 0; i < attributes.size(); i++) {
            if (attribute.equals(attributes.get(i))) {
                return i;
            }
        }
        return attributeIndex;
    }
}
