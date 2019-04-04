package automode.algorithms;

import automode.helper.IndHelper;
import castor.settings.DataModel;

import java.util.HashSet;
import java.util.Set;

public interface AutoMode {

    DataModel runModeBuilder(IndHelper indHelper, int threshold, String thresholdType, String target, String storedProcedure, String dbUrl, String inputIndFile, String outputModeFile, String outputIndFile);

    void runDFS(Vertex<String> vertex, HashSet<Vertex<String>> visited, HashSet<Vertex<String>> candidates, String headMode);

    void findCycles(Graph<String> g, String headMode);

    Set<String> generateGraph(String args);
}
