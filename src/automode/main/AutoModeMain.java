package automode.main;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import automode.util.Constants;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

import automode.db.VoltDBQuery;
import automode.util.Commons;
import automode.util.LogUtil;

public class AutoModeMain {

    protected Graph<String> graph = null;
    protected List<Set<Vertex<String>>> cycles = null;
    protected Map<String, List<Set<String>>> relationSet = null;

    //protected String dbServerURL = "localhost:21212";
    @Option(name = "-dbUrl", usage = "URL of running db instance", required = true)
    public String dbUrl = Constants.Voltdb.URL.getValue();

    @Option(name = "-target", usage = "Target schema", required = true)
    public String target = null;

    @Option(name = "-storedProcedure", usage = "Voltdb procedure", required = true)
    public String storedProcedure = null;

    @Option(name = "-exact", usage = "Exact run")
    public boolean exact = false;

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

    @Option(name = "-outputLog", usage = "Output Automode log path", required = true)
    public String outputLog = null;

    @Option(name = "-threshold", usage = "Threshold value", required = true)
    public int threshold = 0;

    @Option(name = "-thresholdType", usage = "Threshold type abs or pctg", required = true)
    public String thresholdType = null;


    final static Logger logger = Logger.getLogger(AutoModeMain.class);

    AutoModeMain() {
        graph = new Graph<>(true);
        cycles = new ArrayList<>();
        relationSet = new HashMap<>();
        this.threshold = threshold;
    }

    public static void main(String[] args) {
        logger.debug("Inside main");
        AutoModeMain am = new AutoModeMain();
        am.runAutomode(args);
    }

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

        LogUtil.clearLog(outputLog);

        logger.debug("Inside run exact is " + exact + ", approx is " + approx);
        logger.debug("Inside run target is " + target + ", sp is " + storedProcedure);

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

    public void callAutoModeGenerators() {
        AutoModeMain autoMode = null;
        if (exact) {
            logger.debug("Inside exact");
            autoMode = new AutoModeExact();
        }
        if (approx) {
            logger.debug("Inside approx");
            autoMode = new AutoModeApprox();
        }
        autoMode.generateMode(threshold, thresholdType, target, storedProcedure, dbUrl, inputIndFile,  outputModeFile, outputIndFile);
        Commons.resetUniqueVertexTypeGenerator();
    }


    public void generateMode(int threshold, String thresholdType, String target, String storedProcedure, String dbUrl, String inputIndFile,  String outputModeFile, String outputIndFile) { }

    public void generatePredicate(Map<String, List<String>> rel, String type, int threshold, String thresholdType, List<String> modes, String headMode, Set<String> headModeSet) {
        formRelationsSet(rel, threshold, thresholdType, headMode);
        combineRelations(type, modes, headMode, headModeSet);
    }

    public void formRelationsSet(Map<String, List<String>> rel, int threshold, String thresholdType, String headMode) {
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
                updateModeDefinitions(k, col, vertexType, threshold, thresholdType, v.size(), headMode, vQuery);
                vertexTypeSet.add(vertexType);
            }
            if (validIndTable)
                relationSet.put(k, vertexTypeSet);
        });
        vQuery.closeConnection();
    }

    public void updateModeDefinitions(String table, String col, Set<String> vertexType, int threshold, String thresholdType, int noCols, String headMode, VoltDBQuery vQuery) {
        Set<String> temp = new HashSet<>();
        if (table.equals(headMode)) {
            for (String str : vertexType) {
                temp.add(Constants.ModeType.INPUT.getValue() + str);
            }
        } else {
            boolean constant = false;
            if (vQuery.isConstantColumn(table, col, threshold, thresholdType, dbUrl)) {
                logger.debug("Constant Column :: "+table+Constants.Regex.PERIOD.getValue()+col);
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


    public void combineRelations(String type, List<String> modes, String headMode, Set<String> headModeSet) {
        logger.debug("\n ----------------  Predicates " + type + " ------------------------");
        relationSet.forEach((k, v) -> {
            Set[] a = new HashSet[v.size()];
            Set[] o = v.toArray(a);
            List<List<String>> ret = cartesianProduct(k, o);
            if (ret == null) {
                Set ss = o[0];
                for (Object s : ss) {
                    if (k.equals(headMode)) {
                        logger.debug("HEADMODE :: " + k.toLowerCase() + "(" + s + ")");
                        headModeSet.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + s + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                    } else {
                        logger.debug(k.toLowerCase() + "(" + s + ")");
                        modes.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + s + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                    }
                }
            } else {
                for (List<String> s : ret) {
                    if (isModeValid(s) || k.equals(headMode)) {
                        Collections.reverse(s);
                        String str = s.toString().substring(1, s.toString().length() - 1);
                        str = str.replaceAll("\\s", "");
                        if (k.equals(headMode)) {
                            logger.debug("HEADMODE :: " + k.toLowerCase() + "(" + str + ")");
                            headModeSet.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + str + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                        } else {
                            logger.debug(k.toLowerCase() + "(" + str + ")");
                            modes.add(k.toLowerCase() + Constants.Regex.OPEN_PARENTHESIS.getValue() + str + Constants.Regex.CLOSE_PARENTHESIS.getValue());
                        }
                    }
                }
            }
        });
    }

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


    public static List<List<String>> cartesianProduct(String k, Set<String>... sets) {
        List<List<String>> retset = null;
        if (sets.length >= 2)
            retset = _cartesianProduct(0, sets);
        return retset;
    }

    private static List<List<String>> _cartesianProduct(int index, Set<String>... sets) {
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

    public void removeUnwantedRelations(Map<String, List<String>> relations, Set<String> indRelations) {
        relations.keySet().removeIf(e -> (!indRelations.contains(e)));
    }

    public void extractRelationsFromInds(String uNode, String vNode, Set<String> relations){
        uNode = uNode.substring(1, uNode.length()-1);
        vNode = vNode.substring(1, vNode.length()-1);
        String[] un = uNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
        String [] vn = vNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
        relations.add(un[0]);
        relations.add(vn[0]);
    }

    public void printVertexType() {
        logger.debug("\n -----------------Vertex type -------------------------");
        for (Vertex<String> vertex : graph.getAllVertexId()) {
            logger.debug(vertex + " " + vertex.getName() + " Type " + vertex.getVertexType());
        }
    }

    public void printCycle() {
        logger.debug("--------------- Printing Cycles ----------------");
        for (Set<Vertex<String>> v : cycles) {
            logger.debug(v);
        }
    }
}
