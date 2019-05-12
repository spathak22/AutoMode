package automode.profiling;

import automode.db.VoltDBQuery;
import automode.util.Constants;
import automode.util.FileUtil;
import castor.dataaccess.db.DAOFactory;
import castor.dataaccess.db.GenericDAO;
import castor.dataaccess.db.GenericTableObject;
import castor.language.Relation;
import castor.language.Schema;
import castor.language.Tuple;
import castor.utils.TimeWatch;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

public class BinderAlgorithm {

    @Option(name = "-target", usage = "Target schema", required = false)
    public String target = null;

    @Option(name = "-maxerror", usage = "Maximum error", required = true)
    private double maxError = 0.0;

    @Option(name = "-outfile", usage = "Output file", required = true)
    private String outfile;

    @Option(name = "-examplesFile", usage = "Examples file", required = false)
    private String examplesFile = Constants.Regex.EMPTY_STRING.getValue();

    @Option(name = "-examplesRelation", usage = "Examples relation", required = false)
    private String examplesRelation = Constants.Regex.EMPTY_STRING.getValue();

    @Option(name = "-dbUrl", usage = "URL of running db instance", required = false)
    public String dbUrl = Constants.Voltdb.URL.getValue();

    @Option(name = "-port", usage = "Port number of running db instance", required = false)
    public String port = Constants.Voltdb.PORT.getValue();

    @Argument
    private List<String> arguments = new ArrayList<String>();

    final static Logger logger = Logger.getLogger(BinderAlgorithm.class);

    public static void main(String[] args) {
        BinderAlgorithm discovery = new BinderAlgorithm();
        discovery.discoverExactINDsV1(args);
    }


    /*
     * Discovers and prints approximate INDs
     * This version finds overlap between two relations by loading them to memory and computing overlap programatically
     */
    public void discoverExactINDsV1(String[] args) {
        logger.debug("Discovering exact inds using Binder");
        TimeWatch tw = TimeWatch.start();
        //Create list to store all the inds
        List<String> inds = new ArrayList<>();

        // Parse the arguments
        try {
            CmdLineParser parser = new CmdLineParser(this);
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            return;
        }

        String url = dbUrl + ":" + port;

        // Read schema
        VoltDBQuery vQuery = new VoltDBQuery();
        Schema schema = vQuery.getSchema(url);

        if (schema.getRelations().isEmpty()) {
            System.out.println("Error: Database has no relations");
            return;
        }

        DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.VOLTDB);
        try {

            // Create data access objects and set URL of data
            //String dbUrl = "localhost";
            try {
                daoFactory.initConnection(url);
            } catch (RuntimeException e) {
                System.err.println("Unable to connect to server with URL: " + url);
                return;
            }

            //Distinct query template
            String queryTemplate = "select distinct({1}) from {0};";
            GenericDAO genericDAO = daoFactory.getGenericDAO();

            //Bucketize each relation:
            //List<Map<relation.attribute,Set<values>>>
            Map<String, Boolean> indsTracker = new HashMap<>();
            List<Map<String, Set<Tuple>>> relationBucketized = new ArrayList<>();

            logger.debug("Bucketing whole database");

            for (Relation relation : schema.getRelations().values()) {

                for (String attribute : relation.getAttributeNames()) {
                    Map<String, Set<Tuple>> map = new HashMap();
                    String relationQuery = MessageFormat.format(queryTemplate, relation.getName(), attribute);
                    GenericTableObject result = genericDAO.executeQuery(relationQuery);
                    Set<Tuple> relationValues = new HashSet<Tuple>(result.getTable());
                    map.put(relation.getName() + "." + attribute, relationValues);
                    //System.out.println(relation.getName() + "." + attribute);
                    relationBucketized.add(map);
                    indsTracker.put(relation.getName() + "." + attribute, true);
                }
            }

            logger.debug("Running IND Validation");

            for (Map<String, Set<Tuple>> col1 : relationBucketized) {
                for (Map.Entry<String, Set<Tuple>> entry1 : col1.entrySet()) {
                    String key1 = entry1.getKey();
                    Set<Tuple> set1 = entry1.getValue();
                    String[] relationName1 = key1.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));

                    for (Tuple tuple1 : set1) {
                        for (Map<String, Set<Tuple>> col2 : relationBucketized) {
                            for (Map.Entry<String, Set<Tuple>> entry2 : col2.entrySet()) {
                                String key2 = entry2.getKey();
                                Set<Tuple> set2 = entry2.getValue();
                                String[] relationName2 = key2.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));

                                //If same relation then go to next
                                if (relationName1[0].equalsIgnoreCase(relationName2[0]))
                                    continue;

                                //if this relation is already marked as invalid ind then go to next
                                if (!indsTracker.get(key2))
                                    continue;

                                if (set2.contains(tuple1))
                                    continue;
                                else
                                    indsTracker.put(key2, Boolean.FALSE);
                            }
                        }
                    }

                    //Add all valid inds to list
                    for (Map.Entry<String, Boolean> indsEntry : indsTracker.entrySet()) {
                        String key = indsEntry.getKey();
                        String[] relationKey = key.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
//                        Boolean value = indsEntry.getValue();

                        if (relationName1[0].equalsIgnoreCase(relationKey[0])) {
                            continue;
                        }

                        if (indsTracker.get(key)) {
                            inds.add(("(" + key1 + ") < (" + key + ") < " + maxError).toLowerCase());
                        } else {
                            indsTracker.put(key, true);
                        }
                    }
                }
            }
            FileUtil.writeToFile(outfile, inds);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close connection to DBMS
            daoFactory.closeConnection();
        }
        System.out.println("Binder Finished in: " + tw.time() + " ms");
    }

    public List<String> discoverExactINDsV2(Schema schema, String dbUrl, String port) {
        logger.debug("Discovering exact inds using Binder");
        TimeWatch tw = TimeWatch.start();
        //Create list to store all the inds
        List<String> inds = new ArrayList<>();

        String url = dbUrl + ":" + port;

        if (schema.getRelations().isEmpty()) {
            System.out.println("Error: Database has no relations");
            return null;
        }

        DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.VOLTDB);
        try {

            // Create data access objects and set URL of data
            //String dbUrl = "localhost";
            try {
                daoFactory.initConnection(url);
            } catch (RuntimeException e) {
                System.err.println("Unable to connect to server with URL: " + url);
                return null;
            }

            //Distinct query template
            String queryTemplate = "select distinct({1}) from {0};";
            GenericDAO genericDAO = daoFactory.getGenericDAO();

            //Bucketize each relation:
            //List<Map<relation.attribute,Set<values>>>
            Map<String, Boolean> indsTracker = new HashMap<>();
            List<Map<String, Set<Tuple>>> relationBucketized = new ArrayList<>();

            logger.debug("Bucketing whole database");

            for (Relation relation : schema.getRelations().values()) {

                for (String attribute : relation.getAttributeNames()) {
                    Map<String, Set<Tuple>> map = new HashMap();
                    String relationQuery = MessageFormat.format(queryTemplate, relation.getName(), attribute);
                    GenericTableObject result = genericDAO.executeQuery(relationQuery);
                    Set<Tuple> relationValues = new HashSet<Tuple>(result.getTable());
                    map.put(relation.getName() + "." + attribute, relationValues);
                    //System.out.println(relation.getName() + "." + attribute);
                    relationBucketized.add(map);
                    indsTracker.put(relation.getName() + "." + attribute, true);
                }
            }

            logger.debug("Running IND Validation");

            for (Map<String, Set<Tuple>> col1 : relationBucketized) {
                for (Map.Entry<String, Set<Tuple>> entry1 : col1.entrySet()) {
                    String key1 = entry1.getKey();
                    Set<Tuple> set1 = entry1.getValue();
                    String[] relationName1 = key1.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));

                    for (Tuple tuple1 : set1) {
                        for (Map<String, Set<Tuple>> col2 : relationBucketized) {
                            for (Map.Entry<String, Set<Tuple>> entry2 : col2.entrySet()) {
                                String key2 = entry2.getKey();
                                Set<Tuple> set2 = entry2.getValue();
                                String[] relationName2 = key2.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));

                                //If same relation then go to next
                                if (relationName1[0].equalsIgnoreCase(relationName2[0]))
                                    continue;

                                //if this relation is already marked as invalid ind then go to next
                                if (!indsTracker.get(key2))
                                    continue;

                                if (set2.contains(tuple1))
                                    continue;
                                else
                                    indsTracker.put(key2, Boolean.FALSE);
                            }
                        }
                    }

                    //Add all valid inds to list
                    for (Map.Entry<String, Boolean> indsEntry : indsTracker.entrySet()) {
                        String key = indsEntry.getKey();
                        String[] relationKey = key.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
//                        Boolean value = indsEntry.getValue();

                        if (relationName1[0].equalsIgnoreCase(relationKey[0])) {
                            continue;
                        }

                        if (indsTracker.get(key)) {
                            inds.add(("(" + key1 + ") < (" + key + ") < " + maxError).toLowerCase());
                        } else {
                            indsTracker.put(key, true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close connection to DBMS
            daoFactory.closeConnection();
        }

        System.out.println("Binder Finished in: " + tw.time() + " ms");
        return inds;
    }

}
