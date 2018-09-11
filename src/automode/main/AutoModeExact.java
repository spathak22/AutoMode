package automode.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import automode.util.Constants;
import automode.util.FileUtil;
import automode.util.JsonUtil;
import extractschema.main.ExtractVoltDBSchema;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class AutoModeExact extends AutoModeMain {


	@Override
	public void generateMode(int threshold, String thresholdType, String target, String storedProcedure, String dbUrl, String inputIndFile,  String outputModeFile, String outputIndFile) {
		logger.debug("------------------ Generating Exact Mode  -----------------------");
		String inds =  FileUtil.readFile(inputIndFile).toLowerCase();
		List <String> modes = new ArrayList<>();
		Set<String> headModeSet = new HashSet<>();

		//generateGraph
		AutoModeExact am = new AutoModeExact();
		Set<String> indRelations = am.generateGraph(inds);

		//Find and Store all cycles in graph
		am.findCycles(am.graph);
		am.printCycle();
		am.printVertexType();

		//Generate Body Mode
		ExtractVoltDBSchema schema = new ExtractVoltDBSchema();
		Map<String, List<String>> relations = schema.getRelationsMap(dbUrl);
		//This method will keep the only relations present in IND and will remove others
		removeUnwantedRelations(relations,indRelations);
		logger.debug("Relations Size :: "+relations.size());
		am.generatePredicate(relations, Constants.Types.EXACT_IND.getValue(),threshold,thresholdType, modes,target,headModeSet);

		//Generate Head Mode
		String headModeBody = null;
		if(headModeSet.size() == 1) {
			logger.debug("------------------ HeadMode formed correctly  size is 1 -----------------------");
			for(String str: headModeSet)
				headModeBody = str;
		}else{
			logger.error("!!!!!!!!!!!!!!  HeadMode malformed, found more than 1  !!!!!!!!!!!!!!!!!!!!!!!");
			headModeBody = optimiseHeadMode(headModeSet);
		}

		//Generate Outputs
		JsonUtil.writeModeToJsonFormat(null,headModeBody, modes, storedProcedure, outputModeFile);
		if(outputIndFile!=null)
			JsonUtil.writeIndsToJsonFormat(inds,relations,outputIndFile,target);
	}

	public void findCycles(Graph<String> g){
		HashSet<Vertex<String>> visited = new HashSet<>();
		HashSet<Vertex<String>> candidates = new HashSet<>();
		for(Vertex<String> vertex : graph.getAllVertexId()){
			if(! visited.contains(vertex) && !candidates.contains(vertex)){
				logger.debug("\n  --------Assigning Type to  --------"+vertex.getName());
				runDFS(vertex, visited, candidates);
				if(cycles.size() > 0)
					synchCycle();
			}
		}
	}

	private void runDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates) {
		candidates.add(vertex);
		Vertex<String> v1 = graph.getVertexByID(vertex.getId());
		logger.debug(v1.getName() +" >> ");
		for(Vertex<String> adj : vertex.getAdjacentVertexes()) {
  			if(visited.contains(vertex)){
				vertex.addVertexType(adj.getVertexType());
				continue;
			}
			adj.setParent(vertex); // build the trace back
			if(candidates.contains(adj)) {
				addVertexToCycle(candidates);
			} else {
				runDFS(adj, visited, candidates);
				vertex.addVertexType(adj.getVertexType());
			}
		}
		if(vertex.getVertexType().isEmpty())
			vertex.addVertexType(vertex.getId());
		candidates.remove(vertex);
		visited.add(vertex);

		logger.debug(vertex.getName()+ " has Vertex Type  "+vertex.getVertexType());
	}

	private void synchCycle(){
		int size = 0;
		Set<String> vertexType = null;
		//Find largest set in Cycle
		Set<Vertex<String>> cycle = cycles.get(cycles.size()-1);
		for (Vertex<String> vertex: cycle){
			if (size < vertex.getVertexType().size()){
				size = vertex.getVertexType().size();
				vertexType = vertex.getVertexType();
			}
		}
		//Update Cycle with largest set found in previous loop
		for (Vertex<String> vertex: cycle){
			vertex.addVertexType(vertexType);
		}
	}

	private Set<String> generateGraph(String args){
		String[] lines = args.split("\r\n");
		int i = 0;
		//Use this relation to remove unwanted relations fetched from VoltDB
		Set<String> relations = new HashSet();;
		while (i<lines.length){
			String [] line = lines[i].split(Constants.Regex.SUBSET.getValue());
			String uNode = line[0].trim();
			String vVertex = line[1].trim();
			extractRelationsFromInds(uNode,vVertex,relations);
			String [] vNodes = vVertex.trim().split("\\s+");
			int j = 0;
			while(j<vNodes.length){
				String vNode = vNodes[j];
				graph.addEdge(uNode.trim().toLowerCase(), vNode.trim().toLowerCase());
				j++;
			}
			i++;
		}
		return relations;
	}
}