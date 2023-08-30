package modetransform.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.gson.*;
import org.apache.log4j.Logger;

public class FileUtil {
	final static Logger logger = Logger.getLogger(FileUtil.class);

	public static JsonObject convertFileToJSON(String fileName){
        // Read from File to String
        JsonObject jsonObject = null;
        JsonParser parser = new JsonParser();
        JsonElement jsonElement;
		try {
			jsonElement = parser.parse(new FileReader(fileName));
			jsonObject = jsonElement.getAsJsonObject();
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}
        return jsonObject;
    }

	public static String writeModeToJsonFormat(String target, String headMode, List<String> bodyMode, String dbName, String filePath) {
		JsonObject job = new JsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je0 = jp.parse(gson.toJson(target));
		JsonElement je1 = jp.parse(gson.toJson(headMode));
		JsonElement je2 = jp.parse(gson.toJson(bodyMode));
		JsonElement je3 = jp.parse(gson.toJson(dbName));
		job.add("target", je0);
		job.add("headMode", je1);
		job.add("bodyModes", je2);
		job.add("spName", je3);
		try{
			FileWriter file = new FileWriter(filePath);
			logger.debug(" ----------------  Json Output ------------------------");
			file.write(gson.toJson(job));
			file.flush();
			return "Success";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Failed";
	}
}
