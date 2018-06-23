package automode.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import automode.util.Constants;
import automode.util.FileUtil;
import automode.util.JsonUtil;
import extractschema.main.ExtractVoltDBSchema;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class AutoModeApprox extends AutoModeMain {


    @Override
    public void generateMode(int threshold, String thresholdType, String target, String storedProcedure, String dbUrl, String inputIndFile,  String outputModeFile, String outputIndFile) {
        logger.debug("------------------ Generating Approx Mode  -----------------------");
        String inds = FileUtil.readFile(inputIndFile);
        List<String> modes = new ArrayList<>();
        Set<String> headModeSet = new HashSet<>();

        //generateGraph
        AutoModeApprox am = new AutoModeApprox();
        Set<String> indRelations = am.generateGraph(inds);

        //Remove edges(higher epsilon) from two node cycle in the graph
        am.removeHighEpsilonEdges(am.graph);

        //Find and Store all cycles
        am.findCycles(am.graph, target);
        //am.optimiseHeadMode(headMode,am.graph);
        am.printCycle();
        am.printVertexType();

        //Generate Body Mode
        ExtractVoltDBSchema schema = new ExtractVoltDBSchema();
        Map<String, List<String>> relations = schema.getRelationsMap(dbUrl);
        //This method will keep the only relations present in IND and will remove others
        removeUnwantedRelations(relations,indRelations);
        logger.debug("Relations Size :: " + relations.size());
        am.generatePredicate(relations, Constants.Types.APPROX_IND.getValue(), threshold, thresholdType, modes, target, headModeSet);

        //Generate Head Mode
        String headModeBody = null;
        if (headModeSet.size() == 1) {
            logger.debug("------------------ HeadMode formed correctly  size is 1 -----------------------");
            for (String str : headModeSet)
                headModeBody = str;
        } else {
            logger.error("!!!!!!!!!!!!!!  HeadMode malformed, found more than 1  !!!!!!!!!!!!!!!!!!!!!!!");
            headModeBody = optimiseHeadMode(headModeSet);
        }

        //Generate Outputs
        JsonUtil.writeModeToJsonFormat(null, headModeBody, modes, storedProcedure, outputModeFile);
        if (outputIndFile != null)
            JsonUtil.writeIndsToJsonFormat(inds, relations, outputIndFile, target);
    }

    private void removeHighEpsilonEdges(Graph<String> g) {
        Set<Set<Vertex<String>>> epsilonCycle = new HashSet<>();
        List<Vertex<String>> vertexList = new ArrayList<>();
        findEpsilonCycles(g, epsilonCycle);
        for (Set<Vertex<String>> cycleSet : epsilonCycle) {
            if (cycleSet.size() == 2) {
                for (Vertex<String> vertex : cycleSet) {
                    vertexList.add(vertex);
                }
                if (g.getEdgeWeight(vertexList.get(0), vertexList.get(1)) < g.getEdgeWeight(vertexList.get(1), vertexList.get(0))) {
                    logger.debug("Removing Edge " + vertexList.get(1).getName() + " > " + vertexList.get(0).getName());
                    g.removeAdjacentVertex(vertexList.get(1), vertexList.get(0));
                    g.removeEdge(vertexList.get(1), vertexList.get(0));

                } else {
                    logger.debug("Removing Edge " + vertexList.get(0).getName() + " > " + vertexList.get(1).getName());
                    g.removeAdjacentVertex(vertexList.get(0), vertexList.get(1));
                    g.removeEdge(vertexList.get(0), vertexList.get(1));
                }
            }
            vertexList.clear();
        }
    }

    private void findEpsilonCycles(Graph<String> g, Set<Set<Vertex<String>>> epsilonCycle) {
        HashSet<Vertex<String>> visited = new HashSet<>();
        HashSet<Vertex<String>> candidates = new HashSet<>();
        for (Vertex<String> vertex : graph.getAllVertex()) {
            if (!visited.contains(vertex) && !candidates.contains(vertex)) {
                logger.debug("\n --------Searching for edges to be deleted from graph --------" + vertex);
                epsilonDFS(vertex, visited, candidates, epsilonCycle);
            }
        }
    }

    private void epsilonDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates, Set<Set<Vertex<String>>> epsilonCycle) {
        candidates.add(vertex);
        List<Vertex<String>> v = graph.getVertexByID(vertex.getId()).getAdjacentVertexes();
        Vertex<String> v1 = graph.getVertexByID(vertex.getId());
        logger.debug(v1.getName() + " >> ");
        for (Vertex<String> adj : vertex.getAdjacentVertexes()) {
            if (visited.contains(vertex)) {
                continue;
            }
            if (adj.getAdjacentVertexes().contains(vertex)) {
                HashSet<Vertex<String>> cycle = new HashSet<>();
                cycle.add(vertex);
                cycle.add(adj);
                if (!epsilonCycle.contains(cycle))
                    epsilonCycle.add(cycle);
            }
            if (candidates.contains(adj)) {
                addEpsilonVertexToCycle(candidates, epsilonCycle);
            } else {
                epsilonDFS(adj, visited, candidates, epsilonCycle);
            }
        }
        candidates.remove(vertex);
        visited.add(vertex);
    }

    private void addEpsilonVertexToCycle(HashSet<Vertex<String>> candidates, Set<Set<Vertex<String>>> epsilonCycle) {
        HashSet<Vertex<String>> cycle = new HashSet<>();
        cycle.addAll(candidates);
        epsilonCycle.add(cycle);
    }

    private void findCycles(Graph<String> g, String headMode) {
        HashSet<Vertex<String>> visited = new HashSet<Vertex<String>>();
        HashSet<Vertex<String>> candidates = new HashSet<>();
        for (Vertex<String> vertex : graph.getAllVertexId()) {
            if (!visited.contains(vertex) && !candidates.contains(vertex)) {
                logger.debug("\n --------Assigning Type to  --------" + vertex.getName());
                runDFS(vertex, visited, candidates, headMode);
            }
        }
    }

    private void runDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates, String headMode) {
        candidates.add(vertex);
        List<Vertex<String>> v = graph.getVertexByID(vertex.getId()).getAdjacentVertexes();
        Vertex<String> v1 = graph.getVertexByID(vertex.getId());
        logger.debug(v1.getName() + " >> ");

        for (Vertex<String> adj : vertex.getAdjacentVertexes()) {
            if (visited.contains(vertex)) {
                assignVertexType(vertex, adj, headMode);
            } else if (candidates.contains(adj)) {
                addVertexToCycle(candidates);
            } else {
                runDFS(adj, visited, candidates, headMode);
                assignVertexType(vertex, adj, headMode);
            }
        }
        if (vertex.getAdjacentVertexes().isEmpty()) {
            Map<String, Integer> map = new HashMap<String, Integer>();
            map.put(vertex.getId(), 0);
            vertex.addVertexTypeMap(map);
            vertex.addVertexType(vertex.getId());
        }
        candidates.remove(vertex);
        visited.add(vertex);
        logger.debug(vertex.getName() + " has Vertex Type  " + vertex.getVertexType() + " and vertex Id = " + vertex.getId());
    }

    private void assignVertexType(Vertex<String> vertex, Vertex<String> adj, String target) {
        Set<Map<String, Integer>> vertexSet = adj.getVertexTypeMap();
        boolean typeSet = false;
        if (graph.getEdgeWeight(vertex, adj) < Constants.HyperParameters.APPROX_TYPEASSIGN_THRESHOLD.getValue()) {
            for (Map<String, Integer> type : vertexSet) {
                for (Map.Entry<String, Integer> mapEntry : type.entrySet()) {
                    Map<String, Integer> map = new HashMap<String, Integer>();
                    map.put(mapEntry.getKey(), mapEntry.getValue());
                    vertex.addVertexTypeMap(map);
                    vertex.addVertexType(mapEntry.getKey());
                    typeSet = true;
                }
            }
            if (!typeSet) {
                Map<String, Integer> map = new HashMap<String, Integer>();
                map.put(adj.getId(), 0);
                vertex.addVertexTypeMap(map);
                vertex.addVertexType(adj.getId());
                if (!vertex.getName().startsWith(Constants.Regex.OPEN_PARENTHESIS.getValue() + target)) {
                    logger.debug(" *** Added original type to " + adj.getName());
                    adj.setOriginalType(adj.getId());
                }
            }
        } else {
            for (Map<String, Integer> type : vertexSet) {
                for (Map.Entry<String, Integer> mapEntry : type.entrySet()) {
                    if (mapEntry.getValue() == 0) {
                        Map<String, Integer> map = new HashMap<String, Integer>();
                        map.put(mapEntry.getKey(), 1);
                        vertex.addVertexTypeMap(map);
                        vertex.addVertexType(mapEntry.getKey());
                        typeSet = true;
                    }
                }
            }
            if (!typeSet) {
                Map<String, Integer> map = new HashMap<String, Integer>();
                map.put(adj.getId(), 1);
                vertex.addVertexTypeMap(map);
                vertex.addVertexType(adj.getId());
                if (!vertex.getName().startsWith(Constants.Regex.OPEN_PARENTHESIS.getValue() + target)) {
                    logger.debug(" *** Added original type to " + adj.getName());
                    adj.setOriginalType(adj.getId());
                }
            }
        }

    }

    private Set<String> generateGraph(String args) {
        String[] lines = args.split("\r\n");
        int i = 0;
        //Use this relation to remove unwanted relations fetched from VoltDB
        Set<String> relations = new HashSet();;
        while (i < lines.length) {
            String[] line = lines[i].split(Constants.Regex.SUBSET.getValue());
            String uNode = line[0].trim();
            String vVertex = line[1].trim();
            extractRelationsFromInds(uNode,vVertex,relations);
            Double weight = Double.parseDouble(line[2]);
            String[] vNodes = vVertex.trim().split("\\s+");
            int j = 0;
            while (j < vNodes.length) {
                String vNode = vNodes[j];
                graph.addEdge(uNode.trim().toLowerCase(), vNode.trim().toLowerCase(), weight);
                j++;
            }
            i++;
        }
        return relations;
    }

}