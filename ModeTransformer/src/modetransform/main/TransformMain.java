package modetransform.main;

import com.google.gson.JsonObject;
import modetransform.helper.ModeGeneratorHelper;
import modetransform.jsonmodel.DataModel;
import modetransform.jsonmodel.JsonReader;
import modetransform.transformmodel.Relation;
import modetransform.transformmodel.TransformationSchema;
import modetransform.transformmodel.TransformationTuple;
import modetransform.util.Constants;
import modetransform.util.FileUtil;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class TransformMain {
    @Option(name = "-inputPath", usage = "Input file location", required = true)
    private String inputPath;

    @Option(name = "-outputPath", usage = "Output file location", required = true)
    private String outputPath;

    final static Logger logger = Logger.getLogger(TransformMain.class);
    private DataModel dataModel;
    private TransformationSchema tScehma;
    private List<String> modes;



    public static void main(String[] args) {
        // TODO Auto-generated method stub
        TransformMain tm = new TransformMain();
        tm.generateModes(args);
    }


    public void generateModes(String [] args) {
        // Parse the arguments
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            logger.error(e.getMessage());
            parser.printUsage(System.out);
            return;
        }

        logger.info("Processing input");
        // Get input mode information
        JsonObject dataModelJson = FileUtil.convertFileToJSON(inputPath + Constants.FileName.INPUT_MODE_FILE.getValue());
        dataModel = this.readDataModelFromJson(dataModelJson);

        tScehma = new TransformationSchema();
        modes = new ArrayList<String>();
        // Get input tranformation information
        this.readTransformationSchema(inputPath + Constants.FileName.TRANFORMATION_FILE.getValue());
        ModeGeneratorHelper.generateMode(tScehma, modes);
        FileUtil.writeModeToJsonFormat(null, dataModel.getModeH().toString(), modes, dataModel.getSpName(), outputPath + Constants.FileName.OUTPUT_MODE_FILE.getValue());
    }


    public String generateModesUsingTranformation(String dataModelFile,String transformSchema,String outputPath) {
        logger.info("Processing input");
        // Get input mode information
        JsonObject dataModelJson = FileUtil.convertFileToJSON(dataModelFile);
        dataModel = this.readDataModelFromJson(dataModelJson);

        tScehma = new TransformationSchema();
        modes = new ArrayList<String>();
        // Get input tranformation information
        this.readTransformationSchema(transformSchema);
        ModeGeneratorHelper.generateMode(tScehma, modes);
        String result = FileUtil.writeModeToJsonFormat(null, dataModel.getModeH().toString(), modes, dataModel.getSpName(), outputPath + Constants.FileName.OUTPUT_MODE_FILE.getValue());
        if(result.equals("Success"))
            return outputPath + Constants.FileName.OUTPUT_MODE_FILE.getValue();
        return result;
    }


    /*
     * Read data model from JSON object
     */
    private DataModel readDataModelFromJson(JsonObject dataModelJson) {
        DataModel dataModel;
        try {
            logger.info("Reading mode file...");
            dataModel = JsonReader.readDataModel(dataModelJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dataModel;
    }

    private void readTransformationSchema(String transformFile) {
        Stream<String> stream = null;
        try {
            logger.info("Reading Transformation file...");
            stream = Files.lines(Paths.get(transformFile));
            {
                stream.filter(s -> !s.isEmpty()).forEach(s -> processTransformationSchema(s));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error("Error while processing tranformation file");
            e.printStackTrace();
        }
        stream.close();
    }

    private void processTransformationSchema(String lineString) {
        lineString = lineString.replaceAll("\\s", "").toLowerCase();
        String[] line = lineString.split(Constants.TransformDelimeter.ARROW.getValue());
        TransformationTuple transformationTuple = null;
        try {
            List<Relation> sourceRelation = createSourceRelationObject(line[0]);
            List<Relation> targetRelation = createTargetRelationObject(line[1], sourceRelation);
            transformationTuple = new TransformationTuple();
            transformationTuple.setSourceRelation(sourceRelation);
            transformationTuple.setTargetRelation(targetRelation);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.tScehma.addToMembersList(transformationTuple);
    }

    public List<Relation> createSourceRelationObject(String sourceRelationString) {
        logger.info("Creating source relation objects using :: "+sourceRelationString);
        List<Relation> sourceRelationList = new ArrayList<Relation>();
        String[] relations = sourceRelationString.split(Constants.TransformDelimeter.SLASH_CLOSE_PARA.getValue());
        for (String relation : relations) {
            String relationName = relation.substring(0, relation.indexOf(Constants.TransformDelimeter.OPEN_PARA.getValue()));
            if (relationName.startsWith(Constants.TransformDelimeter.COMMA.getValue())) {
                relationName = relation.substring(1, relationName.length());
            }
            Relation sourceRelation = new Relation(relationName);
            String attributes = (String) relation.subSequence(relation.indexOf(Constants.TransformDelimeter.OPEN_PARA.getValue()) + 1, relation.length());
            String[] attributeArray = attributes.split(Constants.TransformDelimeter.COMMA.getValue());
            List<Set<String>> modeTypes = this.dataModel.getModesBMap().get(relationName);
            List<Map<String, Set<String>>> sourceRelationAttributes = new ArrayList<Map<String, Set<String>>>();
            List<Set<String>> attributeTypes = new ArrayList<Set<String>>();
            int count = 0;
            for (String attribute : attributeArray) {
                Map<String, Set<String>> map = new HashMap<String, Set<String>>();
                map.put(attribute, modeTypes.get(count));
                sourceRelationAttributes.add(map);
                attributeTypes.add(modeTypes.get(count));
                count++;
            }
            sourceRelation.setAttributeTypes(attributeTypes);
            sourceRelation.setAttributes(sourceRelationAttributes);
            sourceRelationList.add(sourceRelation);
        }
        return sourceRelationList;
    }


    public List<Relation> createTargetRelationObject(String targetRelationString, List<Relation> sourceRelation) throws Exception {
        logger.info("Creating target relation objects using :: "+targetRelationString);
        List<Relation> targetRelationList = new ArrayList<Relation>();
        String[] relations = targetRelationString.split(Constants.TransformDelimeter.SLASH_CLOSE_PARA.getValue());
        for (String relation : relations) {
            String relationName = relation.substring(0, relation.indexOf(Constants.TransformDelimeter.OPEN_PARA.getValue()));
            if (relationName.startsWith(Constants.TransformDelimeter.COMMA.getValue())) {
                relationName = relation.substring(1, relationName.length());
            }
            String attributes = (String) relation.subSequence(relation.indexOf(Constants.TransformDelimeter.OPEN_PARA.getValue()) + 1, relation.length());
            Relation targetRelation = new Relation(relationName);
            List<Map<String, Set<String>>> targetAttributesList = new ArrayList<Map<String, Set<String>>>();
            List<Set<String>> attributeTypes = new ArrayList<Set<String>>();
            String[] attributeArray = attributes.split(Constants.TransformDelimeter.COMMA.getValue());
            Set<String> attributeType = null;
            for (String attribute : attributeArray) {
                outerloop:for (Relation sourceRel : sourceRelation) {
                    for (Map map : sourceRel.getAttributes()) {
                        if (map.containsKey(attribute)) {
                            attributeType = (Set<String>) map.get(attribute);
                            break outerloop;
                        }
                    }
                }
                if (attributeType != null) {
                    Map<String, Set<String>> map = new HashMap<String, Set<String>>();
                    map.put(attribute, (Set) attributeType);
                    targetAttributesList.add(map);
                    attributeTypes.add((Set) attributeType);
                } else {
                    throw new Exception("Attribute from source relation not found in target");
                }
            }
            //Update attributes types with +/- modes
            attributeTypes = updateAttributeTypes(attributeTypes);
            targetRelation.setAttributeTypes(attributeTypes);
            targetRelation.setAttributes(targetAttributesList);
            targetRelationList.add(targetRelation);
        }
        return targetRelationList;
    }


    public List<Set<String>> updateAttributeTypes(List<Set<String>> typeSet) {
        List<Set<String>> typeSetList = new ArrayList<Set<String>>();
        for (Set<String> set : typeSet) {
            Set<String> temp = new HashSet<String>();
            for (String str : set) {
                if (!str.startsWith(Constants.ModeType.CONSTANT.getValue())) {
                    temp.add(Constants.ModeType.INPUT.getValue() + str);
                    if (typeSet.size() > 1) {
                        temp.add(Constants.ModeType.OUTPUT.getValue() + str);
                    }
                } else {
                    temp.add(str);
                }
            }
            typeSetList.add(temp);
        }
        return typeSetList;
    }

}
