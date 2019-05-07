package automode.algorithms;

import automode.db.VoltDBQuery;
import automode.util.Commons;
import automode.util.Constants;
import castor.language.Mode;
import castor.settings.DataModel;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

public abstract class AutoModeImpl implements AutoMode {

    protected Graph<String> graph = null;
    protected List<Set<Vertex<String>>> cycles = null;
    //Relation set consists of relations in format - relation({+T1,+T2},{-T3,#T3})
    //protected Map<String, List<Set<String>>> relationSet = null;

    final static Logger logger = Logger.getLogger(AutoModeImpl.class);

    AutoModeImpl() {
        graph = new Graph<>(true);
        cycles = new ArrayList<>();
        //protected Map<String, List<Set<String>>> relationSet = new HashMap<>();
        //this.threshold = threshold;
    }


    /**
     * Building modes is a two step process
     * 1. Create relations
     * 2. Generate mode definitions
     */
    public DataModel buildModes(String examplesRelation, Map<String, List<String>> rel, String type, int threshold, String thresholdType, String headMode, String dbUrl, String spName) {

        //1. Create Relation set consists of relations in format as : relation({+T1,+T2},{-T3,#T3})
        Map<String, List<Set<String>>> relationSet = createRelationsTypeSet(rel, threshold, thresholdType, examplesRelation, dbUrl);

        //2. Generate mode definitions i.e. all possible combinations as : relation(+T1,-T3), relation(+T1,#T3).....
        return generateModeDeffintion(examplesRelation, relationSet, type, headMode, spName);
    }

    /**
     * Method to create RelationsType Set
     */
    public Map<String, List<Set<String>>> createRelationsTypeSet(Map<String, List<String>> rel, int threshold, String thresholdType, String headMode, String dbUrl) {
        Map<String, List<Set<String>>> relationSet = new HashMap<>();
        VoltDBQuery vQuery = new VoltDBQuery();
        logger.debug("Relations Size :: " + rel.size());
        rel.forEach((k, v) -> {
            List<Set<String>> vertexTypeSet = new ArrayList<Set<String>>();
            boolean validIndTable = false;
            for (String col : v) {
                //logger.debug(k.toLowerCase() + "[" + col.toLowerCase() + "]");
                Vertex<String> vertex = graph.getVertexByName(Constants.Regex.OPEN_PARENTHESIS.getValue() + k.toLowerCase() + Constants.Regex.PERIOD.getValue() + col.toLowerCase() + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                //logger.debug(Constants.Regex.OPEN_PARENTHESIS.getValue() + k.toLowerCase() + Constants.Regex.PERIOD.getValue() + col.toLowerCase() + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                Set<String> vertexType = null;
                if (vertex == null) {
                    vertexType = Commons.getUniqueVertexTypeSet();
                } else {
                    //logger.debug("Original type "+vertex.getOriginalType());
                    validIndTable = true;
                    vertexType = vertex.getVertexType();
                    if (vertex.getOriginalType() != null)
                        vertexType.add(vertex.getOriginalType());

                }
                //addIdentifierType will add Identifier types to each type such as it will append +/-/# to type T1.
                addIdentifierType(k, col, vertexType, threshold, thresholdType, v.size(), headMode, vQuery, dbUrl);
                vertexTypeSet.add(vertexType);
            }
            if (validIndTable)
                relationSet.put(k, vertexTypeSet);
        });
        vQuery.closeConnection();
        return relationSet;
    }

    /**
     * Method to add Identifier type
     */
    public void addIdentifierType(String table, String col, Set<String> vertexType, int threshold, String thresholdType, int noCols, String headMode, VoltDBQuery vQuery, String dbUrl) {
        Set<String> temp = new HashSet<>();
        if (table.equals(headMode)) {
            for (String str : vertexType) {
                temp.add(Constants.ModeType.INPUT.getValue() + str);
            }
        } else {
            boolean constant = false;
            if (vQuery.isConstantColumn(table, col, threshold, thresholdType, dbUrl)) {
                logger.debug("Constant Column :: " + table + Constants.Regex.PERIOD.getValue() + col);
                constant = true;
            }
            for (String str : vertexType) {
                temp.add(Constants.ModeType.INPUT.getValue() + str);
                if (noCols > 1) {
                    temp.add(Constants.ModeType.OUTPUT.getValue() + str);
                    if (constant) {
                        temp.add(Constants.ModeType.CONSTANT.getValue() + str);
                    }
                }
            }
        }
        vertexType.clear();
        vertexType.addAll(temp);
    }

    /**
     * Method to generate Mode Definition recursively
     */
    public DataModel generateModeDeffintion(String examplesRelation, Map<String, List<Set<String>>> relationSet, String type, String headMode, String spName) {
        logger.debug("\n ----------------  Predicates " + type + " ------------------------");
        Set<String> headModeSet = new HashSet<>();
        List<Mode> modes = new ArrayList<>();

        relationSet.forEach((k, v) -> {
            Set[] a = new HashSet[v.size()];
            Set[] o = v.toArray(a);
            List<List<String>> ret = cartesianProduct(k, o);
            if (ret == null) {
                Set ss = o[0];
                for (Object s : ss) {
                    if (k.equalsIgnoreCase(examplesRelation)) {
                        logger.debug("HEADMODE :: " + k.toLowerCase() + "(" + s + ")");
                        headModeSet.add(s.toString());
                    } else {
                        logger.debug(k.toLowerCase() + "(" + s + ")");
                        modes.add(Mode.stringToMode(k.toLowerCase() + "(" + s.toString() + ")"));
                        //modes.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + s + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                    }
                }
            } else {
                for (List<String> s : ret) {
                    if (isModeValid(s) || k.equalsIgnoreCase(examplesRelation)) {
                        Collections.reverse(s);
                        String str = s.toString().substring(1, s.toString().length() - 1);
                        str = str.replaceAll("\\s", "");
                        if (k.equalsIgnoreCase(examplesRelation)) {
                            logger.debug("HEADMODE :: " + k.toLowerCase() + "(" + str + ")");
                            headModeSet.add(str);
                            //headModeSet.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + str + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                        } else {
                            logger.debug(k.toLowerCase() + "(" + str + ")");
                            modes.add(Mode.stringToMode(k.toLowerCase() + "(" + str + ")"));
                            //modes.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + str + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                        }
                    }
                }
            }
        });

        //Generate Head Mode
        Mode headModeBody = null;

        //null check
        String target = null;
        if (headMode != null) {
            target = headMode.toLowerCase();
        } else {
            return new DataModel(null, modes, spName);
        }

        if (headModeSet.size() == 1) {
            logger.debug("------------------ HeadMode formed correctly  size is 1 -----------------------");
            for (String s : headModeSet) {
                headModeBody = Mode.stringToMode(target + "(" + s.toString() + ")");
            }
        } else {
            logger.error("!!!!!!!!!!!!!!  HeadMode malformed, found more than 1  !!!!!!!!!!!!!!!!!!!!!!!");
            headModeBody = Mode.stringToMode(target + "(" + optimiseHeadMode(headModeSet) + ")");
        }

        //Initialize and return new dataModel
        return new DataModel(headModeBody, modes, spName);
    }

//    public List<Argument> convertStringToArguments(String modeString){
//         String [] types = modeString.split("Constants.Regex.COMMA.getValue()");
//        List<Argument> argumentList = new ArrayList<>();
//        for (String type: types){
//            argumentList.add(new Argument(modeString));
//        }
//        return argumentList;
//    }

/*
    public IdentifierType getIdentifierType(String idType){
        if(idType.equals(IdentifierType.INPUT))
            return IdentifierType.INPUT;
        else if(idType.equals(IdentifierType.OUTPUT))
            return IdentifierType.OUTPUT;
        else
            return IdentifierType.CONSTANT;
    }
*/


    /**
     * Add vertex to cycles
     */
    public void addVertexToCycle(HashSet<Vertex<String>> candidates) {
        Iterator<Set<Vertex<String>>> iterator = cycles.iterator();
        while (iterator.hasNext()) {
            Set<Vertex<String>> set = iterator.next();
            if (set.containsAll(candidates)) {
                return;
            } else if (candidates.containsAll(set)) {
                iterator.remove();
            }
        }
        HashSet<Vertex<String>> cycle = new HashSet<>();
        cycle.addAll(candidates);
        cycles.add(cycle);
    }

    /**
     * Verify if mode is valid
     */
    public boolean isModeValid(List<String> ls) {
        int plus = 0;
        for (String str : ls) {
            if (str.startsWith(Constants.ModeType.INPUT.getValue()))
                plus++;
        }
        if (plus == 0 || plus > 1)
            return false;
        return true;
    }

    /**
     * Generated cartesian product of attribute types
     */
    public static List<List<String>> cartesianProduct(String k, Set<String>... sets) {
        List<List<String>> retset = null;
        if (sets.length >= 2)
            retset = _cartesianProduct(0, sets);
        return retset;
    }

    /**
     * Generated cartesian product using sets
     */
    public static List<List<String>> _cartesianProduct(int index, Set<String>... sets) {
        List<List<String>> ret = new ArrayList<List<String>>();
        if (index == sets.length) {
            ret.add(new ArrayList<>());
        } else {
            for (String obj : sets[index]) {
                for (List<String> set : _cartesianProduct(index + 1, sets)) {
                    set.add(obj);
                    ret.add(set);
                }
            }
        }
        return ret;
    }

    /**
     * Remove relations not present in Inds
     */
    public void removeUnwantedRelations(Map<String, List<String>> relations, Set<String> indRelations) {
        relations.keySet().removeIf(e -> (!indRelations.contains(e)));
    }

    /**
     * Method to extract relation names from inclusion dependencies
     */
    public void extractRelationsFromInds(String uNode, String vNode, Set<String> relations) {
        uNode = uNode.substring(1, uNode.length() - 1);
        vNode = vNode.substring(1, vNode.length() - 1);
        String[] un = uNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
        String[] vn = vNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
        relations.add(un[0]);
        relations.add(vn[0]);
    }

    /**
     * Print vertex types
     */
    public void printVertexType() {
        logger.debug("\n -----------------Vertex type -------------------------");
        for (Vertex<String> vertex : graph.getAllVertexId()) {
            logger.debug(vertex + " " + vertex.getName() + " Type " + vertex.getVertexType());
        }
    }

    /**
     * Print cycles
     */
    public void printCycle() {
        logger.debug("--------------- Printing Cycles ----------------");
        for (Set<Vertex<String>> v : cycles) {
            logger.debug(v);
        }
    }

    /**
     * Remove the multiple head modes
     */
    public String optimiseHeadMode(Set<String> headModeSet) {
        logger.debug("!!!! Fixing HeadMode !!!!");
        List<Set> termsList = new ArrayList<>();
        //String targetRelation = null;

        for (String str : headModeSet) {
//            String[] termsUnformatted = str.split(Constants.Regex.SPLITON_OPEN_PARENTHESIS.getValue());
//            if (targetRelation == null) {
//                targetRelation = termsUnformatted[0];
//            }
//            String terms = termsUnformatted[1].substring(0, termsUnformatted[1].indexOf(Constants.Regex.CLOSE_PARENTHESIS.getValue()));
            String[] termsArray = str.split(Constants.Regex.COMMA.getValue());
            int index = 0;

            for (String term : termsArray) {
                if (termsList.size() != termsArray.length) {
                    Set<String> set = new HashSet();
                    set.add(term);
                    termsList.add(set);
                } else {
                    Set<String> set = termsList.get(index);
                    set.add(term);
                }
                index++;
            }
        }

        Map<String, String> visitedTerms = new HashMap<>();
        List<String> targetMode = new ArrayList<>();

        for (Set<String> set : termsList) {
            for (String term : set) {
                String key = getSetItemsPartOfUpdatedMap(set, visitedTerms);
                if (key != null) {
                    targetMode.add(visitedTerms.get(key));
                    updateMap(set, visitedTerms.get(key), visitedTerms);
                } else {
                    targetMode.add(term);
                    updateMap(set, term, visitedTerms);
                }
                break;
            }
        }

        return String.join(Constants.Regex.COMMA.getValue(), targetMode);
        //return targetRelation + Constants.Regex.OPEN_PARENTHESIS.getValue() + targetTerms + Constants.Regex.CLOSE_PARENTHESIS.getValue();
    }

    /**
     * Intermediate helper methods
     */
    public void updateMap(Set<String> set, String value, Map<String, String> map) {
        for (String term : set) {
            map.put(term, value);
        }
    }

    /**
     * Intermediate helper methods
     */
    public String getSetItemsPartOfUpdatedMap(Set<String> set, Map<String, String> map) {
        if (map.isEmpty())
            return null;
        for (String term : set) {
            if (map.containsKey(term))
                return term;
        }
        return null;
    }

}
