package automode.util;

import java.io.IOException;
import java.io.PrintWriter;

public class LogUtil {
	public static void clearLog(String path){
        try{ 
        	PrintWriter writer = new PrintWriter(path);
        	writer.print("");
        	writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
