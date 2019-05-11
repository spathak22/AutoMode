package automode.algorithms;

import automode.helper.IndHelper;
import automode.util.Constants;
import automode.util.FileUtil;
import castor.dataaccess.file.CSVFileReader;
import castor.settings.DataModel;
import extractschema.main.ExtractVoltDBSchema;

import java.util.*;

public class AutoModeApproximate extends AutoModeImpl {

    /**
     *  Method to initialize mode generation
     */
    @Override
    public DataModel runModeBuilder(String examplesFile, String examplesRelation, IndHelper indHelper, int threshold, String thresholdType, String target, String storedProcedure, String dbUrl, String inputIndFile, String outputModeFile, String outputIndFile){
        logger.debug("------------------ Generating Approx Mode  -----------------------");
        String inds = FileUtil.readFile(inputIndFile).toLowerCase();
//        List<String> modes = new ArrayList<>();
//        Set<String> headModeSet = new HashSet<>();

        //generateGraph
        //AutoModeApproximate am = new AutoModeApproximate();
        Set<String> indRelations = this.generateGraph(inds);

        //Remove edges(higher epsilon) from two node cycle in the graph
        this.removeHighEpsilonEdges(this.graph);

        //Find and Store all cycles
        this.findCycles(this.graph, examplesRelation);
        //am.optimiseHeadMode(headMode,am.graph);
        this.printCycle();
        this.printVertexType();

        //Generate Body Mode
        ExtractVoltDBSchema schema = new ExtractVoltDBSchema();
        Map<String, List<String>> relations = schema.getRelationsMap(dbUrl);
        //Add examplesRelation to relations for fileTypeInput
        if(!(examplesFile.isEmpty()))
            relations.put(examplesRelation,CSVFileReader.readCSVHeader(examplesFile));

        //This method will keep the only relations present in IND and will remove others
        removeUnwantedRelations(relations, indRelations);
        logger.debug("Relations Size :: " + relations.size());
        DataModel dataModel = this.buildModes(examplesRelation, relations, Constants.Types.APPROX_IND.getValue(), threshold, thresholdType, target, dbUrl, storedProcedure);
        indHelper.setDbRelations(relations);
        indHelper.setInds(inds);
        return dataModel;
    }

    /**
     *  Method to Remove edges with high ind error in epsilon edge flow
     */
    public void removeHighEpsilonEdges(Graph<String> g) {
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

    /**
     *  Method to find cycles in epsilon edge flow
     */
    public void findEpsilonCycles(Graph<String> g, Set<Set<Vertex<String>>> epsilonCycle) {
        HashSet<Vertex<String>> visited = new HashSet<>();
        HashSet<Vertex<String>> candidates = new HashSet<>();
        for (Vertex<String> vertex : graph.getAllVertex()) {
            if (!visited.contains(vertex) && !candidates.contains(vertex)) {
                logger.debug("\n --------Searching for edges to be deleted from graph --------" + vertex);
                epsilonDFS(vertex, visited, candidates, epsilonCycle);
            }
        }
    }

    /**
     *  Run DFS in epsilon edge flow
     */
    public void epsilonDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates, Set<Set<Vertex<String>>> epsilonCycle) {
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

    /**
     *  Add cycle in epsilon edge flow
     */
    public void addEpsilonVertexToCycle(HashSet<Vertex<String>> candidates, Set<Set<Vertex<String>>> epsilonCycle) {
        HashSet<Vertex<String>> cycle = new HashSet<>();
        cycle.addAll(candidates);
        epsilonCycle.add(cycle);
    }

    /**
     *  Find cycle in mode generation flow
     */
    public void findCycles(Graph<String> g, String headMode) {
        HashSet<Vertex<String>> visited = new HashSet<Vertex<String>>();
        HashSet<Vertex<String>> candidates = new HashSet<>();
        for (Vertex<String> vertex : graph.getAllVertexId()) {
            if (!visited.contains(vertex) && !candidates.contains(vertex)) {
                logger.debug("\n --------Assigning Type to  --------" + vertex.getName());
                runDFS(vertex, visited, candidates, headMode);
            }
        }
    }

    /**
     *  runDFS for modegeneration flow
     */
    public void runDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates, String headMode) {
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

    /**
     *  Create Vertex type in mode generation flow
     */
    public void assignVertexType(Vertex<String> vertex, Vertex<String> adj, String target) {
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