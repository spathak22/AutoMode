package automode.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import automode.util.Commons;
import automode.util.Constants;
import automode.util.JsonUtil;

public class Graph<T>{

    private List<Edge<T>> allEdges;
    private Map<String,Vertex<T>> allVertex;
    private Map<String,Vertex<T>> allVertexId;
    private boolean isDirected = false;
    
    public Graph(boolean isDirected){
        this.allEdges = new ArrayList<>();
        this.allVertex = new HashMap<>();
        this.allVertexId = new HashMap<>();
        this.isDirected = isDirected;
    }
    
    public void addEdge(String id1, String id2){
        addEdge(id1,id2,0.0);
    }
 
    public Double getEdgeWeight(Vertex<T> vertex1, Vertex<T> vertex2){
    	for (Edge<T> edge: allEdges){
    		if(edge.getVertex1().equals(vertex1) && edge.getVertex2().equals(vertex2)){
    			return edge.getWeight();
    		}
    	}
		return 0.0;
    }

    public void removeEdge(Vertex<T> vertex1, Vertex<T> vertex2){
        int index = 0;
        for (Edge<T> edge: allEdges){
            if(edge.getVertex1().equals(vertex1) && edge.getVertex2().equals(vertex2)){
                break;
            }
            index++;
        }
        allEdges.remove(index);
    }

    public String reverseEdgesToInds(){
        StringBuilder sb = new StringBuilder();
        for (Edge<T> edge: allEdges){
            sb.append(edge.getVertex1().getName()+" < "+edge.getVertex2().getName()+ "\r\n");
        }
        return sb.toString();
    }

    public void removeAdjacentVertex(Vertex<T> vertex1, Vertex<T> vertex2){
    	vertex1.getAdjacentVertexes().remove(vertex2);
    }
    
    //This works only for directed graph because for undirected graph we can end up
    //adding edges two times to allEdges
    public void addVertex(Vertex<T> vertex){
        if(allVertex.containsKey(vertex.getId())){
            return;
        }
        allVertex.put(vertex.getId(), vertex);
        for(Edge<T> edge : vertex.getEdges()){
            allEdges.add(edge);
        }
    }
    
    public Vertex<T> addSingleVertex(String id){
        if(allVertex.containsKey(id)){
            return allVertex.get(id);
        }
        Vertex<T> v = new Vertex<T>(id);
        allVertex.put(id, v);
        return v;
    }
    
    public Vertex<T> getVertexByID(String id){
        return allVertexId.get(id);
    }
  
    public Vertex<T> getVertexByName(String id){
        return allVertex.get(id);
    }
    
    
    public void addEdge(String id1,String id2, Double weight){
        Vertex<T> vertex1 = null;
        if(allVertex.containsKey(id1)){
            vertex1 = allVertex.get(id1);
        }else{
        	String newId = Commons.getUniqueVertexType();
            vertex1 = new Vertex<T>(newId);
            allVertex.put(id1, vertex1);
            allVertexId.put(newId, vertex1);
            vertex1.setName(id1);
        }
        Vertex<T> vertex2 = null;
        if(allVertex.containsKey(id2)){
            vertex2 = allVertex.get(id2);
        }else{
        	String newId = Commons.getUniqueVertexType();
            vertex2 = new Vertex<T>(newId);
            allVertex.put(id2, vertex2);
            allVertexId.put(newId, vertex2);
            vertex2.setName(id2);
        }

        Edge<T> edge = new Edge<T>(vertex1,vertex2,isDirected,weight);
        allEdges.add(edge);
        vertex1.addAdjacentVertex(edge, vertex2);

        if(!isDirected){
            vertex2.addAdjacentVertex(edge, vertex1);
        }

    }
    
    public String formatString(String str){
		String [] strArr = str.split(Constants.Regex.PERIOD.getValue());
		String fstr = strArr[0]+strArr[1];
		return fstr;
    }

    public List<Edge<T>> getAllEdges(){
        return allEdges;
    }
    
    public Collection<Vertex<T>> getAllVertex(){
        return allVertex.values();
    }
    
    
    public Collection<Vertex<T>> getAllVertexId(){
        return allVertexId.values();
    }
    
    public void setDataForVertex(long id, T data){
        if(allVertex.containsKey(id)){
            Vertex<T> vertex = allVertex.get(id);
            vertex.setData(data);
        }
    }

    @Override
    public String toString(){
        StringBuffer buffer = new StringBuffer();
        for(Edge<T> edge : getAllEdges()){
            buffer.append(edge.getVertex1() + " " + edge.getVertex2() + " " + edge.getWeight());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}


class Vertex<T> {
	private String id;
	private String name;
    private T data;
    private Vertex<T> parent;
    private List<Edge<T>> edges = new ArrayList<Edge<T>>();
    private List<Vertex<T>> adjacentVertex = new ArrayList<Vertex<T>>();
    private Set<Map<String,Integer>> vertextTypeMap = new HashSet<Map<String,Integer>> ();
    private Set<String> vertextType = new HashSet<String> ();
    private String originalType;

    public String getOriginalType() {
        return originalType;
    }

    public void setOriginalType(String originalType) {
        this.originalType = originalType;
    }

    public void addVertexType(String str){
        this.vertextType.add(str);
    }
   
    public void addVertexType(Set<String> set){
        for(String s: set){
            this.vertextType.add(s);
        }
    }
   
    public void updateVertexType(Vertex<T> v, String str){
        this.vertextType.removeAll(vertextType);
       
        this.vertextType.add(str);
    }
   
    public void removeVertexType(String str){
        this.vertextType.remove(str);
    }
   
    public  Set<String> getVertexType(){
        return this.vertextType;
    }
    
    public void addVertexTypeMap(Map<String,Integer> str){
    	this.vertextTypeMap.add(str);
    }
    
    public void addVertexTypeMap(Set<Map<String,Integer>> set){
    	for(Map<String,Integer> s: set){
    		this.vertextTypeMap.add(s);
    	}
    }
    
    public void updateVertexTypeMap(Vertex<T> v, Map<String,Integer> str){
    	this.vertextTypeMap.removeAll(vertextType);
    	this.vertextTypeMap.add(str);
    } 
    
    public void removeVertexTypeMap(String str){
    	this.vertextTypeMap.remove(str);
    }
    
    public  Set<Map<String,Integer>> getVertexTypeMap(){
    	return this.vertextTypeMap;
    }

    Vertex(String id){
        this.id = id;
    }
    
    public String getId(){
        return id;
    }
    
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setData(T data){
        this.data = data;
    }
    
    public T getData(){
        return data;
    }
    
    public void addAdjacentVertex(Edge<T> e, Vertex<T> v){
        this.edges.add(e);
        this.adjacentVertex.add(v);
    }
    
    public String toString(){
        return String.valueOf(id);
    }
    
    public List<Vertex<T>> getAdjacentVertexes(){
        return adjacentVertex;
    }
    
    public List<Edge<T>> getEdges(){
        return edges;
    }
    
    public int getDegree(){
        return edges.size();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vertex other = (Vertex) obj;
        if (id != other.id)
            return false;
        return true;
    }

	public Vertex<T> getParent() {
		return this.parent;
	}

	public void setParent(Vertex<T> parent) {
		this.parent = parent;
	}
}

class Edge<T>{
    private boolean isDirected = false;
    private Vertex<T> vertex1;
    private Vertex<T> vertex2;
    private Double weight;
    
    Edge(Vertex<T> vertex1, Vertex<T> vertex2){
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
    }

    Edge(Vertex<T> vertex1, Vertex<T> vertex2,boolean isDirected,Double weight){
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.weight = weight;
        this.isDirected = isDirected;
    }
    
    Edge(Vertex<T> vertex1, Vertex<T> vertex2,boolean isDirected){
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.isDirected = isDirected;
    }
    
    Vertex<T> getVertex1(){
        return vertex1;
    }
    
    Vertex<T> getVertex2(){
        return vertex2;
    }
    
    Double getWeight(){
        return weight;
    }
    
    public boolean isDirected(){
        return isDirected;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((vertex1 == null) ? 0 : vertex1.hashCode());
        result = prime * result + ((vertex2 == null) ? 0 : vertex2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Edge other = (Edge) obj;
        if (vertex1 == null) {
            if (other.vertex1 != null)
                return false;
        } else if (!vertex1.equals(other.vertex1))
            return false;
        if (vertex2 == null) {
            if (other.vertex2 != null)
                return false;
        } else if (!vertex2.equals(other.vertex2))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Edge [isDirected=" + isDirected + ", vertex1=" + vertex1
                + ", vertex2=" + vertex2 + ", weight=" + weight + "]";
    }
}