package automode.util;

import castor.dataaccess.db.GenericDAO;
import castor.dataaccess.db.GenericTableObject;
import castor.dataaccess.file.CSVFileReader;
import castor.language.Relation;
import castor.language.Tuple;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Commons {

	private static Integer seq = 0;
	public static Set<String> getUniqueVertexTypeSet(){
		seq = seq + 1;
		Set <String> s = new HashSet<String>();
		s.add(Constants.Types.TYPE_PREFIX.getValue()+seq);
		return s;
	} 
	
	public static String getUniqueVertexType(){
		seq = seq + 1;
		return Constants.Types.TYPE_PREFIX.getValue()+seq;
	}
	
	public static void resetUniqueVertexTypeGenerator(){
		seq = 0;
	}

	/*
    * This method is equivalent of executeDAOQuery for running "select distinct({1}) from {0};" on example files
    **/
	public static GenericTableObject getDistinctExamplesFromFile(String examplesFile, Relation relation, String attribute) {
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

	public static boolean isEmtpyRelation(String relationName, GenericDAO genericDAO){
		String queryTemplate = "select count(*) from {0};";
		String query = MessageFormat.format(queryTemplate, relationName);
		GenericTableObject result = genericDAO.executeQuery(query);
		if(Integer.parseInt(result.getTable().get(0).getStringValues().get(0))==0)
			return true;
		return false;
	}
}
