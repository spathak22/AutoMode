package automode.algorithms;

import automode.util.Constants;
import castor.language.Mode;
import castor.language.Relation;
import castor.language.Schema;
import castor.settings.DataModel;
import castor.settings.JsonSettingsReader;
import castor.utils.FileUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

//import castor.language.Mode;


public class AutoModePerQuery extends AutoModeApproximate {

    final static Logger logger = Logger.getLogger(AutoModePerQuery.class);
    /**
     *   Go through each entry in Inclusion dependency file
     *   Structure for headmode: headMode(T1, T2, T3)
     *   Create new headModeTypeMap --> Map<String, List<Set<String>>> headModeMap
     *   Fill the List<Set<String>>> with the type information found in headModeBody
     *   Now Run the mode deffinition generator on headModeMap
     */
    public DataModel connectHeadToBodyModes(String target, String schemaJson, String modesJson, String indsText) {
        //     String inds =  FileUtil.readFile(indsText).toLowerCase();
        Schema schema = JsonSettingsReader.readSchema(FileUtils.convertFileToJSON(schemaJson));
        DataModel dataModel = JsonSettingsReader.readRelaxedDataModel(FileUtils.convertFileToJSON(modesJson));
        List<Set<String>> argumentList = new ArrayList<>();
        Set<String> visitedSet = new HashSet<>();

        Stream<String> stream = null;
        try {
            logger.info("Reading Inds file...");
            stream = Files.lines(Paths.get(indsText));
            {
                stream.filter(s -> !s.isEmpty()).forEach(ind -> processInds(schema, ind, dataModel, visitedSet, argumentList));
            }
        } catch (IOException e) {
            logger.error("Error while processing Inds file");
            e.printStackTrace();
        }
        stream.close();

        //Generate HeadMode
        Set[] a = new HashSet[argumentList.size()];
        Set[] o = argumentList.toArray(a);
        Set<String> headModeSet = generateMode(target, o);

        //Generate Head Mode
        Mode headModeBody = null;
        if (headModeSet.size() == 1) {
            logger.debug("------------------ HeadMode formed correctly  size is 1 -----------------------");
            for (String s : headModeSet)
                headModeBody = Mode.stringToMode(target + "(" + s.toString() + ")");
        } else {
            logger.error("!!!!!!!!!!!!!!  HeadMode malformed, found more than 1  !!!!!!!!!!!!!!!!!!!!!!!");
            headModeBody = Mode.stringToMode(target + "(" + optimiseHeadMode(headModeSet) + ")");
        }

        //Initialize and return new dataModel
        dataModel.setModeH(headModeBody);
        return dataModel;
    }

    /**
     *  Go through each entry in Inclusion dependency file
     *  Structure for headmode: headMode(T1, T2, T3)
     *   Create new headModeTypeMap --> Map<String, List<Set<String>>> headModeMap
     *  Fill the List<Set<String>>> with the type information found in headModeBody
     *  Now Run the mode deffinition generator on headModeMap
     *  (QUERY1_ALL.ID) < (COT_EVENT_POSITION.ID) < 0.0
     *  (QUERY1_ALL.ID) < (COT_EVENT.ID) < 0.0
     *  (QUERY1_ALL.SOURCE_ID) < (COT_EVENT.SOURCE_ID) < 0.0
     *  (QUERY1_ALL.SOURCE_ID) < (SOURCE.SOURCE_ID) < 0.0
     *  (QUERY1_ALL.COT_TYPE) < (COT_EVENT.COT_TYPE) < 0.0
     */
    public void processInds(Schema schema, String indString, DataModel dataModel, Set<String> visitedSet, List<Set<String>> arugumentList) {
        //Assign type to each attribute in target query and  come up with final mode
        //List<Argument> argumentList = dataModel.getModeH().getArguments();
        String formattedInd = indString.trim().replaceAll("\\s", "").toUpperCase();
        String[] ind = formattedInd.split("<");
        String[] ind1 = ind[0].split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
        String[] ind2 = ind[1].split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
        String taregtRelation = ind2[0].substring(1);
        String targetAttribute = ind2[1].substring(0, ind2[1].length() - 1);
        String sourceAttribute = ind1[1].substring(0, ind1[1].length() - 1);
        Set<String> argumentSet;

        if (!visitedSet.contains(sourceAttribute)) {
            argumentSet = new HashSet<>();
            arugumentList.add(argumentSet);
            visitedSet.add(sourceAttribute);
        } else {
            argumentSet = arugumentList.get(arugumentList.size() - 1);
        }
        addTypeInfoToArgumentSet(argumentSet, schema.getRelations(), dataModel, taregtRelation, targetAttribute);
    }

    /**
     * add type to Argument set
     */
    public void addTypeInfoToArgumentSet(Set<String> argumentSet, Map<String, Relation> relations, DataModel dataModel, String relation, String attribute) {
        List<List<String>> modesList = dataModel.getModesBMap().get(relation.toLowerCase());
        int attributeIndex = getAttributePositionInRelation(relations, relation, attribute);
        for (int i = 0; i < modesList.size(); i++) {
            argumentSet.add("+"+modesList.get(i).get(attributeIndex).substring(1));
        }
    }

    /**
     * Get position of an attribute in relation
     */
    public int getAttributePositionInRelation(Map<String, Relation> relations, String relation, String attribute) {
        List<String> attributes = relations.get(relation).getAttributeNames();
        int attributeIndex = -1;
        for (int i = 0; i < attributes.size(); i++) {
            if (attribute.equals(attributes.get(i))) {
                return i;
            }
        }
        return attributeIndex;
    }

    /**
     * call recursive mode generation flow
     */
    public Set<String> generateMode(String k, Set<String>... o) {
        Set<String> headModeSet = new HashSet<>();
        List<List<String>> ret = cartesianProduct(k, o);
        for (List<String> s : ret) {
            Collections.reverse(s);
            String str = s.toString().substring(1, s.toString().length() - 1);
            str = str.replaceAll("\\s", "");
            logger.debug("HEADMODE :: " + k.toLowerCase() + "(" + str + ")");
            headModeSet.add(str);
        }
        return headModeSet;
    }


}
