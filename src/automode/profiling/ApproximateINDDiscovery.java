package automode.profiling;

import automode.db.VoltDBQuery;
import automode.util.Constants;
import automode.util.FileUtil;
import castor.dataaccess.db.DAOFactory;
import castor.dataaccess.db.GenericDAO;
import castor.dataaccess.db.GenericTableObject;
import castor.dataaccess.db.VoltDBConnectionContainer;
import castor.dataaccess.file.CSVFileReader;
import castor.language.Relation;
import castor.language.Schema;
import castor.language.Tuple;
import castor.utils.TimeWatch;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public class ApproximateINDDiscovery {

    @Option(name = "-target", usage = "Target schema", required = false)
    public String target = null;

    @Option(name = "-maxerror", usage = "Maximum error", required = true)
    private double maxError;

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

    public static void main(String[] args) {
        ApproximateINDDiscovery discovery = new ApproximateINDDiscovery();
        discovery.discoverApproximateINDsV2(args);
    }

	/*
     * Discovers and prints approximate INDs
	 * This version finds overlap between two relations by joining relations in DB
	 */
//	public void discoverApproximateINDs(String[] args) {
//		TimeWatch tw = TimeWatch.start();
//
//		// Parse the arguments
//        try {
//        	CmdLineParser parser = new CmdLineParser(this);
//			parser.parseArgument(args);
//        } catch (CmdLineException e) {
//			System.err.println(e.getMessage());
//			return;
//		}
//
//		// Read JSON object
//        //JsonObject schemaJson = FileUtils.convertFileToJSON(schemaFile);
//
//        // Read schema
////        Schema schema;
////        try {
////			schema = JsonSettingsReader.readSchema(schemaJson);
////		} catch (Exception e) {
////			e.printStackTrace();
////			return;
////		}
//
//		DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.VOLTDB);
//        try {
//        	// Create data access objects and set URL of data
//        	String dbUrl = "localhost";
//        	try {
//        		daoFactory.initConnection(dbUrl);
//        	}
//        	catch (RuntimeException e) {
//        		System.err.println("Unable to connect to server with URL: " + dbUrl);
//        		return;
//        	}
//    		GenericDAO genericDAO = daoFactory.getGenericDAO();
//
//    		String numeratorQueryTemplate = "select count(distinct(r.{1})) from (select {1} from {0}) r, (select {3} from {2}) s where r.{1} = s.{3};";
//    		String denominatorQueryTemplate = "select count(distinct({1})) from {0};";
//    		for (Relation relation1 : schema.getRelations().values()) {
//    		    for (String attribute1 : relation1.getAttributeNames()) {
//
//    		    	for (Relation relation2 : schema.getRelations().values()) {
//    		    		for (String attribute2 : relation2.getAttributeNames()) {
//
//    		    			// If same relation and attribute, continue
//    		    			if (relation1.getName().equals(relation2.getName()) && attribute1.equals(attribute2))
//    		    				continue;
//
//    		    			String numeratorQuery = MessageFormat.format(numeratorQueryTemplate, relation1.getName(), attribute1, relation2.getName(), attribute2);
//    		        		String denominatorQuery = MessageFormat.format(denominatorQueryTemplate, relation1.getName(), attribute1);
//
//    		        		long numerator = genericDAO.executeScalarQuery(numeratorQuery);
//    		        		long denominator = genericDAO.executeScalarQuery(denominatorQuery);
//
//    		        		if (denominator == 0) {
//    		        			continue;
//    		        		}
//
//    		        		double error = 1.0 - ((double)numerator/(double)denominator);
//
//    		        		if (error <= maxError)
//    		        			System.out.println(relation1.getName()+"["+attribute1+"] < "+ relation2.getName()+"["+attribute2+"] - error: "+error);
//    		    		}
//    		    	}
//    		    }
//    		}
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        finally {
//        	// Close connection to DBMS
//        	daoFactory.closeConnection();
//        }
//
//        System.out.println("Finished in: "+tw.time()+" ms");
//	}

    /*
     * Discovers and prints approximate INDs
     * This version finds overlap between two relations by loading them to memory and computing overlap programatically
     */
    public void discoverApproximateINDsV2(String[] args) {
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
            //String dbUrl = "localhost";
            try {
                daoFactory.initConnection(url);
            } catch (RuntimeException e) {
                System.err.println("Unable to connect to server with URL: " + url);
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
            //System.out.println("examplesFile :: " + examplesFile + " examplesRelation:: " + examplesRelation + " examplesRelationSuffix:: " + examplesRelationSuffix);

            if (!examplesFile.isEmpty()) {
                examplesRelation = FilenameUtils.getBaseName(new File(examplesFile).getName());
                List<String> examplesFileHeader = CSVFileReader.readCSVHeader(examplesFile);

                //System.out.print(" examplesRelation:: " + examplesRelation);
                schema.getRelations().put(examplesRelation, new Relation(examplesRelation, examplesFileHeader));
            }

            for (Relation relation1 : schema.getRelations().values()) {

//                if (!examplesRelationSuffix.isEmpty() && (relation1.getName().toLowerCase().endsWith(examplesRelationSuffix.toLowerCase()) && !relation1.getName().equalsIgnoreCase(examplesRelation)))
//                    continue;

                for (String attribute1 : relation1.getAttributeNames()) {

                    for (Relation relation2 : schema.getRelations().values()) {

                        // If same relation continue
                        if (relation1.getName().equalsIgnoreCase(relation2.getName())) //|| (!examplesRelationSuffix.isEmpty() && (relation2.getName().toLowerCase().endsWith(examplesRelationSuffix.toLowerCase()) && !relation2.getName().equalsIgnoreCase(examplesRelation))))
                            continue;

                        for (String attribute2 : relation2.getAttributeNames()) {

                            String leftRelationQuery = MessageFormat.format(queryTemplate, relation1.getName(), attribute1);
                            String rightRelationQuery = MessageFormat.format(queryTemplate, relation2.getName(), attribute2);

                            GenericTableObject leftResult = null;
                            if (relation1.getName().equalsIgnoreCase(examplesRelation) && !examplesFile.isEmpty()) {
                                if(!cache.containsKey(relation1+"."+attribute1)) {
                                    leftResult = this.getDistinctExamplesFromFile(examplesFile, relation1, attribute1);
                                    cache.put(relation1+"."+attribute1,leftResult);
                                }else{
                                    leftResult = cache.get(relation1+"."+attribute1);
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
                            if (relation2.getName().equalsIgnoreCase(examplesRelation) && !examplesFile.isEmpty()) {
                                if(!cache.containsKey(relation2+"."+attribute2)) {
                                    rightResult = this.getDistinctExamplesFromFile(examplesFile, relation2, attribute2);
                                    cache.put(relation2+"."+attribute2,rightResult);
                                }else{
                                    rightResult=cache.get(relation2+"."+attribute2);
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

                            if (error <= maxError)
                                //System.out.println(relation1.getName()+"["+attribute1+"] < "+ relation2.getName()+"["+attribute2+"] - error: "+error);
                                inds.add(("(" + relation1.getName() + "." + attribute1 + ") < (" + relation2.getName() + "." + attribute2 + ") < " + error).toLowerCase());
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
