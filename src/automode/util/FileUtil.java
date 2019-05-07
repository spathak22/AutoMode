package automode.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class FileUtil {
	
	public static String readFile(String fpath){
		FileReader fr = null;
		StringBuilder sb = new StringBuilder();
		try {
			fr = new FileReader(fpath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				sb.append(line+" \r\n");
			}
			fr.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void writeToFile(String filename, List<String> lines) {
		try {
			File file = new File(filename);
			FileOutputStream fos = FileUtils.openOutputStream(file,false);
			fos.close();
			Path filePath = Paths.get(filename);
			Files.write(filePath, lines, Charset.defaultCharset());
		} catch (IOException var3) {
			throw new RuntimeException(var3.getMessage());
		}
	}
}
