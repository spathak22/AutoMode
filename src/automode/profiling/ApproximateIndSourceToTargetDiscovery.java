package automode.profiling;

import automode.db.VoltDBQuery;
import automode.util.Constants;
import castor.dataaccess.db.DAOFactory;
import castor.dataaccess.db.GenericDAO;
import castor.dataaccess.db.GenericTableObject;
import castor.dataaccess.file.CSVFileReader;
import castor.language.Relation;
import castor.language.Schema;
import castor.language.Tuple;
import castor.utils.FileUtils;
import automode.util.FileUtil;
import castor.utils.TimeWatch;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

public class ApproximateIndSourceToTargetDiscovery {
    @Option(name = "-maxerror", usage = "Maximum error", required = true)
    private double maxError;

    @Option(name = "-outfile", usage = "Output file", required = true)
    private String outfile;

    @Option(name = "-target", usage = "Target relation", required = true)
    private String target;

    @Option(name = "-dbUrl", usage = "URL of running db instance", required = false)
    public String dbUrl = Constants.Voltdb.URL.getValue();

    @Option(name = "-port", usage = "Port number of running db instance", required = false)
    public String port = Constants.Voltdb.PORT.getValue();

    @Option(name = "-examplesFile", usage = "Examples file", required = false)
    private String examplesFile = Constants.Regex.EMPTY_STRING.getValue();

    @Option(name = "-examplesRelation", usage = "Examples relation", required = false)
    private String examplesRelation = Constants.Regex.EMPTY_STRING.getValue();


    @Argument
    private List<String> arguments = new ArrayList<String>();

    public static void main(String[] args) {
        ApproximateIndSourceToTargetDiscovery discovery = new ApproximateIndSourceToTargetDiscovery();
        discovery.discoverApproximateINDsSourceToTarget(args);
    }


    public void discoverApproximateINDsSourceToTarget(String[] args) {
        TimeWatch tw = TimeWatch.start();
        Map<String,GenericTableObject> cache = new HashMap<>();

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

        DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.VOLTDB);
        try {
            //Create list to store all the inds
            List<String> inds = new ArrayList<>();
            // Create data access objects and set URL of data
            String dbUrl = "localhost";
            try {
                daoFactory.initConnection(dbUrl);
            } catch (RuntimeException e) {
                System.err.println("Unable to connect to server with URL: " + dbUrl);
                return;
            }
            GenericDAO genericDAO = daoFactory.getGenericDAO();
            String queryTemplate = "select distinct({1}) from {0};";

            //Remove unwanted example relations for ind discovery
            if(!target.isEmpty()){
                schema.getRelations().entrySet().removeIf(entry -> entry.getKey().toLowerCase().startsWith(target.toLowerCase())
                        && !entry.getKey().equalsIgnoreCase(examplesRelation));
            }

            //Check if examplesFile is given
            Relation sourceRelation = null;
            if (!examplesFile.isEmpty()) {
                examplesRelation = FilenameUtils.getBaseName(new File(examplesFile).getName());
                List<String> examplesFileHeader = CSVFileReader.readCSVHeader(examplesFile);
                sourceRelation = new Relation(examplesRelation, examplesFileHeader);
            } else {
                sourceRelation = getTargetRelationFromDBSchema(examplesRelation);
            }

            //Relation sourceRelation = getTargetRelationFromDBSchema(target);

            for (String attribute1 : sourceRelation.getAttributeNames()) {

                for (Relation targetRelation : schema.getRelations().values()) {

                    // If same relation and attribute, continue
                    if (targetRelation.getName().equalsIgnoreCase(sourceRelation.getName()))// || (!examplesRelationSuffix.isEmpty() && (targetRelation.getName().toLowerCase().endsWith(examplesRelationSuffix.toLowerCase()) && !targetRelation.getName().equalsIgnoreCase(examplesRelation))))
                        continue;

                    for (String attribute2 : targetRelation.getAttributeNames()) {

                        String leftRelationQuery = MessageFormat.format(queryTemplate, sourceRelation.getName(), attribute1);
                        String rightRelationQuery = MessageFormat.format(queryTemplate, targetRelation.getName(), attribute2);

                        GenericTableObject leftResult = null;
                        if (sourceRelation.getName().equalsIgnoreCase(examplesRelation) && !examplesFile.isEmpty()) {
                            if(!cache.containsKey(sourceRelation+"."+attribute1)) {
                                leftResult = this.getDistinctExamplesFromFile(examplesFile, sourceRelation, attribute1);
                                cache.put(sourceRelation+"."+attribute1,leftResult);
                            }else{
                                leftResult = cache.get(sourceRelation+"."+attribute1);
                            }
                        } else {
                            leftResult = genericDAO.executeQuery(leftRelationQuery);
                        }

                        int leftAttributeCount = leftResult.getTable().size();

                        // If denominator is 0, skip
                        if (leftAttributeCount == 0) {
                            continue;
                        }

                        GenericTableObject rightResult = null;
                        if (targetRelation.getName().equalsIgnoreCase(examplesRelation) && !examplesFile.isEmpty()) {
                            if(!cache.containsKey(targetRelation+"."+attribute2)) {
                                rightResult = this.getDistinctExamplesFromFile(examplesFile, targetRelation, attribute2);
                                cache.put(targetRelation+"."+attribute2,rightResult);
                            }else{
                                rightResult=cache.get(targetRelation+"."+attribute2);
                            }
                        } else {
                            rightResult = genericDAO.executeQuery(rightRelationQuery);
                        }

                        Set<Tuple> leftRelationValues = new HashSet<Tuple>(leftResult.getTable());
                        Set<Tuple> rightRelationValues = new HashSet<Tuple>(rightResult.getTable());

                        int intersectionCount = 0;

                        for (Tuple tuple : leftRelationValues) {
                            if (rightRelationValues.contains(tuple))
                                intersectionCount++;
                        }

                        double error = 1.0 - ((double) intersectionCount / (double) leftAttributeCount);

                        if (error <= maxError) {
                            //System.out.println(relation1.getName()+"["+attribute1+"] < "+ relation2.getName()+"["+attribute2+"] - error: "+error);
                            inds.add(("(" + sourceRelation.getName() + "." + attribute1 + ") < (" + targetRelation.getName() + "." + attribute2 + ") < " + error).toLowerCase());
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

        System.out.println("Finished in: " + tw.time() + " ms");
    }

    public Relation getTargetRelationFromDBSchema(String target) {
        VoltDBQuery vQuery = new VoltDBQuery();
        Schema schema = vQuery.getSchema(dbUrl);
        for (Relation targetRelation : schema.getRelations().values()) {
            if (targetRelation.getName().equalsIgnoreCase(target)) {
                return targetRelation;
            }
        }
        return null;
    }

    /*
* This method is equivalent of executeDAOQuery for running "select distinct({1}) from {0};" on example files
**/
    public GenericTableObject getDistinctExamplesFromFile(String examplesFile, Relation relation, String attribute) {
        Set<String> visitedSet = new HashSet<>();
        List<Tuple> resultSet = new ArrayList<>();

        List<Tuple> tuples = CSVFileReader.readCSV(examplesFile);
        int index = relation.getAttributeNames().indexOf(attribute);

        for (Tuple tuple : tuples) {
            if (!visitedSet.contains(tuple.getStringValues().get(index))) {
                List<Object> row = new ArrayList();
                visitedSet.add(tuple.getStringValues().get(index));
                row.add(tuple.getStringValues().get(index));
                resultSet.add(new Tuple(row));
            }
        }
        return new GenericTableObject(resultSet);
    }

}
