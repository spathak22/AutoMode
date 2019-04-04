package automode.algorithms;

import automode.helper.IndHelper;
import automode.util.Constants;
import automode.util.FileUtil;
import castor.settings.DataModel;
import extractschema.main.ExtractVoltDBSchema;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoModeExact extends AutoModeImpl {

    /**
     *  Method to initialize mode generation
     */
    @Override
    public DataModel runModeBuilder(IndHelper indHelper, int threshold, String thresholdType, String target, String storedProcedure, String dbUrl, String inputIndFile, String outputModeFile, String outputIndFile) {
        logger.debug("------------------ Generating Exact Mode  -----------------------");
        String inds = FileUtil.readFile(inputIndFile).toLowerCase();
//		List <String> modes = new ArrayList<>();
//		Set<String> headModeSet = new HashSet<>();

        //generateGraph from inclusion dependencies
        //AutoModeExact am = new AutoModeExact();
        Set<String> indRelations = this.generateGraph(inds);

        //Find and Store all cycles in graph
        this.findCycles(this.graph, target);
        this.printCycle();
        this.printVertexType();

        //Generate Body Mode
        ExtractVoltDBSchema schema = new ExtractVoltDBSchema();
        Map<String, List<String>> relations = schema.getRelationsMap(dbUrl);
        //This method will keep the only relations present in IND and will remove others
        removeUnwantedRelations(relations, indRelations);
        logger.debug("Relations Size :: " + relations.size());
        DataModel dataModel = this.buildModes(relations, Constants.Types.EXACT_IND.getValue(), threshold, thresholdType, target, dbUrl, storedProcedure);
        indHelper.setDbRelations(relations);
        indHelper.setInds(inds);
        return dataModel;
    }

    /**
     *  Find cycle in mode generation flow
     */
    public void findCycles(Graph<String> g, String target) {
        HashSet<Vertex<String>> visited = new HashSet<>();
        HashSet<Vertex<String>> candidates = new HashSet<>();
        for (Vertex<String> vertex : graph.getAllVertexId()) {
            if (!visited.contains(vertex) && !candidates.contains(vertex)) {
                logger.debug("\n  --------Assigning Type to  --------" + vertex.getName());
                runDFS(vertex, visited, candidates, target);
                if (cycles.size() > 0)
                    synchCycle();
            }
        }
    }

    /**
     *  runDFS for modegeneration flow
     */
    public void runDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates, String target) {
        candidates.add(vertex);
        Vertex<String> v1 = graph.getVertexByID(vertex.getId());
        logger.debug(v1.getName() + " >> ");
        for (Vertex<String> adj : vertex.getAdjacentVertexes()) {
            if (visited.contains(vertex)) {
                vertex.addVertexType(adj.getVertexType());
                continue;
            }
            adj.setParent(vertex); // build the trace back
            if (candidates.contains(adj)) {
                addVertexToCycle(candidates);
            } else {
                runDFS(adj, visited, candidates, target);
                vertex.addVertexType(adj.getVertexType());
            }
        }
        if (vertex.getVertexType().isEmpty())
            vertex.addVertexType(vertex.getId());
        candidates.remove(vertex);
        visited.add(vertex);

        logger.debug(vertex.getName() + " has Vertex Type  " + vertex.getVertexType());
    }

    /**
     *  Synchronize the cycle
     */
    public void synchCycle() {
        int size = 0;
        Set<String> vertexType = null;
        //Find largest set in Cycle
        Set<Vertex<String>> cycle = cycles.get(cycles.size() - 1);
        for (Vertex<String> vertex : cycle) {
            if (size < vertex.getVertexType().size()) {
                size = vertex.getVertexType().size();
                vertexType = vertex.getVertexType();
            }
        }
        //Update Cycle with largest set found in previous loop
        for (Vertex<String> vertex : cycle) {
            vertex.addVertexType(vertexType);
        }
    }

    /**
     *  Generate graph from inclusion depndencies
     */
    public Set<String> generateGraph(String args) {
        String[] lines = args.split("\r\n");
        int i = 0;
        //Use this relation to remove unwanted relations fetched from VoltDB
        Set<String> relations = new HashSet();
        ;
        while (i < lines.length) {
            String[] line = lines[i].split(Constants.Regex.SUBSET.getValue());
            String uNode = line[0].trim();
            String vVertex = line[1].trim();
            extractRelationsFromInds(uNode, vVertex, relations);
            String[] vNodes = vVertex.trim().split("\\s+");
            int j = 0;
            while (j < vNodes.length) {
                String vNode = vNodes[j];
                graph.addEdge(uNode.trim().toLowerCase(), vNode.trim().toLowerCase());
                j++;
            }
            i++;
        }
        return relations;
    }
}