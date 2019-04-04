package automode.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;


public class JsonUtil {

	final static Logger logger = Logger.getLogger(JsonUtil.class);

	public static void writeIndsToJsonFormat(String ind, Map<String, List<String>> r, String filePath, String headMode){
		JsonObject jobParent = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String[] lines = ind.split("\r\n");
		int i = 0;
		while (i<lines.length){
			String [] line = lines[i].split(Constants.Regex.SUBSET.getValue());
			String uNode = line[0].trim().toLowerCase();
			uNode = uNode.substring(1, uNode.length()-1);
			String vNode = line[1].trim().toLowerCase();
			vNode = vNode.substring(1, vNode.length()-1);
			if(uNode.contains(headMode) || vNode.contains(headMode)){
				i++;
				continue;
			}
			JsonObject jobChild = new JsonObject();
			String [] un = uNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
			String [] vn = vNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
			int lindex = r.get(un[0].trim()).indexOf(un[1].trim());
			int rindex = r.get(vn[0].trim()).indexOf(vn[1].trim());
			jobChild.addProperty(Constants.Inds.LEFT_RELATION.getValue(), un[0]);
			jobChild.addProperty(Constants.Inds.LEFT_ATTRIBUTE_NUMBER.getValue(), lindex);
			jobChild.addProperty(Constants.Inds.RIGHT_RELATION.getValue(), vn[0]);
			jobChild.addProperty(Constants.Inds.RIGHT_ATTRIBUTE_NUMBER.getValue(), rindex);
			JsonElement element = gson.fromJson(jobChild.toString(), JsonElement.class);
			jsonArray.add(element);
			i++;
		}
		jobParent.add("inds", jsonArray);
		try{ 
			FileWriter file = new FileWriter(filePath);
			logger.debug(" ----------------  Json Output ------------------------");
			file.write(gson.toJson(jobParent));
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void writeModeToJsonFormat(String target, String headMode, List<String> bodyMode, String dbName, String filePath) {
    	JsonObject job = new JsonObject();
    	Gson gson = new GsonBuilder().setPrettyPrinting().create();
    	JsonParser jp = new JsonParser();
    	JsonElement je0 = jp.parse(gson.toJson(target));
    	JsonElement je1 = jp.parse(gson.toJson(headMode));
    	JsonElement je2 = jp.parse(gson.toJson(bodyMode));
    	JsonElement je3 = jp.parse(gson.toJson(dbName));
    	job.add(Constants.Modes.TARGET.getValue(), je0);
    	job.add(Constants.Modes.HEAD_MODE.getValue(), je1);
    	job.add(Constants.Modes.BODY_MODES.getValue(), je2);
    	job.add(Constants.Modes.SP_NAME.getValue(), je3);
        try{ 
        	FileWriter file = new FileWriter(filePath);
			logger.debug(" ----------------  Json Output ------------------------");
        	file.write(gson.toJson(job));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.print(job);
    }
}
