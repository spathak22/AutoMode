package automode.util;

import java.util.HashSet;
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
}
