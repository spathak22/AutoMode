package modetransform.jsonmodel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import modetransform.util.Constants;

public class JsonReader {

	/*
	 * Read JSON object for data model and convert to object
	 */
	public static DataModel readDataModel(JsonObject dataModelJson) throws Exception {
		Mode modeH;
		List<Mode> modesB;
		Map<String,List<Set<String>>> modesBMap;
		String spName;
		
		// Read head mode
		if (dataModelJson.get("headMode") == null) {
			throw new Exception("Head mode not set in data model json.");
		} else {
			String modeHString = dataModelJson.get("headMode").getAsString();
			modeH = Mode.stringToMode(modeHString);
		}
		
		// Read body modes
		if (dataModelJson.get("bodyModes") == null) {
			throw new Exception("Body modes not set in data model json.");
		} else {
			modesB = new LinkedList<Mode>();
			modesBMap = new HashMap<String,List<Set<String>>>();
			JsonArray modesBArray = dataModelJson.get("bodyModes").getAsJsonArray();
			for (int i = 0; i < modesBArray.size(); i++) {
				String modebString = modesBArray.get(i).getAsString();
				modesB.add(Mode.stringToMode(modebString));
				initializeModesBodyMap(modebString,modesBMap);
			}
		}
		
		// Read stored prodecure name
		if (dataModelJson.get("spName") == null) {
			throw new Exception("Stored procedure name not set in data model json.");
		} else {
			spName = dataModelJson.get("spName").getAsString();
		}
		
		return new DataModel(modeH, modesB, modesBMap, spName);
	}

	//Add the attribute type information to jsonmodel, add #attribute directly. Remove +/- from string
	public static void initializeModesBodyMap(String modebString, Map<String,List<Set<String>>> modesBMap){
		String relationName = modebString.substring(0,modebString.indexOf(Constants.TransformDelimeter.OPEN_PARA.getValue()));
		String  attributes = (String) modebString.subSequence(modebString.indexOf(Constants.TransformDelimeter.OPEN_PARA.getValue())+1, modebString.indexOf(Constants.TransformDelimeter.CLOSE_PARA.getValue()));
		String [] attributeArray = attributes.split(Constants.TransformDelimeter.COMMA.getValue());
		int count = 0;
		List<Set<String>> attributeTypeList = null;
		if(!modesBMap.containsKey(relationName)) {
			attributeTypeList = new ArrayList<Set<String>>();
		} else{
			attributeTypeList = modesBMap.get(relationName);
		}
		for(String attribute: attributeArray){
			if(!attribute.startsWith(Constants.ModeType.CONSTANT.getValue())){
				attribute = attribute.substring(1);
			}
			if(!modesBMap.containsKey(relationName)){
				Set <String> s = new HashSet<String>();
				s.add(attribute);
				attributeTypeList.add(s);
			}else{
				attributeTypeList.get(count).add(attribute);
			}
			count++;
		}
		modesBMap.put(relationName,attributeTypeList);
	}
	
}
