/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2016 Oliver Lum
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package oarlib.teavm;

import oarlib.graph.impl.DirectedGraph;
import oarlib.graph.impl.MixedGraph;
import oarlib.graph.impl.UndirectedGraph;
import oarlib.graph.impl.WindyGraph;
import oarlib.util.SimpleLogger;

import java.util.*;

/**
 * TeaVM-compatible OARLIB file parser.
 * Parses OARLIB format files provided as strings instead of file I/O.
 * Supports Windy graph format.
 */
public class OARLibParser {

    private static final SimpleLogger LOGGER = SimpleLogger.getLogger(OARLibParser.class);

    /**
     * Parse OARLIB format string and create a WindyGraph
     *
     * @param content The full OARLIB file content as a string
     * @return A WindyGraph instance populated from the file content
     * @throws Exception if parsing fails
     */
    public static WindyGraph parseWindyGraph(String content) throws Exception {
        LOGGER.info("Parsing OARLIB content...");
        
        String[] lines = content.split("\n");
        Map<String, String> metadata = new HashMap<>();
        List<LinkData> links = new ArrayList<>();
        List<VertexData> vertices = new ArrayList<>();
        
        boolean inLinksSection = false;
        boolean inVerticesSection = false;
        int numVertices = 0;
        int numEdges = 0;
        int depotId = 1;
        
        // Parse metadata and data sections
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }
            
            // Parse metadata headers
            if (line.contains("Graph Type:")) {
                metadata.put("graphType", extractValue(line));
                continue;
            }
            if (line.contains("Depot ID")) {
                try {
                    depotId = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    depotId = 1;
                }
                continue;
            }
            if (line.contains("N:")) {
                try {
                    numVertices = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    // ignore
                }
                continue;
            }
            if (line.contains("M:")) {
                try {
                    numEdges = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    // ignore
                }
                continue;
            }
            
            // Section markers
            if (line.contains("LINKS") && !line.contains("END")) {
                inLinksSection = true;
                inVerticesSection = false;
                continue;
            }
            if (line.contains("VERTICES") && !line.contains("END")) {
                inLinksSection = false;
                inVerticesSection = true;
                continue;
            }
            if (line.contains("END LINKS") || line.contains("END VERTICES")) {
                inLinksSection = false;
                inVerticesSection = false;
                continue;
            }
            
            // Parse link data
            if (inLinksSection && !line.contains("Format:")) {
                try {
                    LinkData linkData = parseLinkLine(line);
                    if (linkData != null) {
                        links.add(linkData);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse link: " + line);
                }
            }
            
            // Parse vertex data
            if (inVerticesSection && !line.contains("Format:")) {
                try {
                    VertexData vertexData = parseVertexLine(line);
                    if (vertexData != null) {
                        vertices.add(vertexData);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse vertex: " + line);
                }
            }
        }
        
        // Create graph if we have valid data
        if (numVertices <= 0 && !vertices.isEmpty()) {
            numVertices = vertices.size();
        }
        if (numVertices <= 0) {
            numVertices = 150; // Default
        }
        
        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }

        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }

        LOGGER.info("Creating WindyGraph with " + numVertices + " vertices and " + links.size() + " edges");

        WindyGraph graph = new WindyGraph(numVertices);
        
        // Add edges
        for (LinkData link : links) {
            try {
                graph.addEdge(link.v1, link.v2, link.cost, link.reverseCost, link.isRequired);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link);
            }
        }
        
        // Add vertex coordinates if available
        for (VertexData vertex : vertices) {
            try {
                if (vertex.id > 0 && vertex.id <= numVertices) {
                    graph.getVertex(vertex.id).setCoordinates(vertex.x, vertex.y);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to set vertex coordinates: " + vertex);
            }
        }
        
        // Set depot
        try {
            graph.setDepotId(depotId);
        } catch (Exception e) {
            graph.setDepotId(1);
        }
        
        LOGGER.info("Graph parsing complete. Vertices: " + graph.getVertices().size() + 
                    ", Edges: " + graph.getEdges().size());
        
        return graph;
    }
    
    /**
     * Parse a link line from OARLIB format
     * Format: V1,V2,COST,REVERSE_COST,isRequired
     */
    private static LinkData parseLinkLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length >= 5) {
                LinkData data = new LinkData();
                data.v1 = Integer.parseInt(parts[0].trim());
                data.v2 = Integer.parseInt(parts[1].trim());
                data.cost = Integer.parseInt(parts[2].trim());
                data.reverseCost = Integer.parseInt(parts[3].trim());
                data.isRequired = Boolean.parseBoolean(parts[4].trim());
                return data;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
    
    /**
     * Parse a vertex line from OARLIB format
     * Format: x,y
     */
    private static VertexData parseVertexLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                VertexData data = new VertexData();
                data.x = Double.parseDouble(parts[0].trim());
                data.y = Double.parseDouble(parts[1].trim());
                return data;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
    
    /**
     * Extract value after colon in metadata lines
     */
    private static String extractValue(String line) {
        int idx = line.indexOf(":");
        if (idx >= 0 && idx < line.length() - 1) {
            return line.substring(idx + 1).trim();
        }
        return "";
    }
    
    /**
     * Inner class to hold link data
     */
    static class LinkData {
        int v1, v2, cost, reverseCost;
        boolean isRequired;
        
        @Override
        public String toString() {
            return "LinkData{" +
                    "v1=" + v1 +
                    ", v2=" + v2 +
                    ", cost=" + cost +
                    ", reverseCost=" + reverseCost +
                    ", isRequired=" + isRequired +
                    '}';
        }
    }
    
    /**
     * Parse DirectedGraph from OARLIB content string
     */
    public static DirectedGraph parseDirectedGraph(String content) throws Exception {
        LOGGER.info("Parsing OARLIB content for DirectedGraph...");
        
        String[] lines = content.split("\n");
        List<LinkData> links = new ArrayList<>();
        int numVertices = 0;
        int depotId = 1;
        
        boolean inLinksSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }
            
            if (line.contains("N:")) {
                try {
                    numVertices = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    // ignore
                }
                continue;
            }
            
            if (line.contains("Depot ID")) {
                try {
                    depotId = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    depotId = 1;
                }
                continue;
            }
            
            if (line.contains("LINKS") && !line.contains("END")) {
                inLinksSection = true;
                continue;
            }
            
            if (line.contains("END LINKS") || line.contains("VERTICES")) {
                inLinksSection = false;
                continue;
            }
            
            if (inLinksSection) {
                try {
                    LinkData linkData = parseLinkLine(line);
                    if (linkData != null) {
                        links.add(linkData);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse link: " + line);
                }
            }
        }
        
        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }
        
        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }
        
        LOGGER.info("Creating DirectedGraph with " + numVertices + " vertices and " + links.size() + " edges");
        
        DirectedGraph graph = new DirectedGraph(numVertices);
        
        for (LinkData link : links) {
            try {
                graph.addEdge(link.v1, link.v2, "edge", link.cost, true);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link);
            }
        }
        
        try {
            graph.setDepotId(depotId);
        } catch (Exception e) {
            graph.setDepotId(1);
        }
        
        LOGGER.info("Graph parsing complete. Vertices: " + graph.getVertices().size() + 
                    ", Edges: " + graph.getEdges().size());
        
        return graph;
    }

    /**
     * Parse UndirectedGraph from OARLIB content string
     */
    public static UndirectedGraph parseUndirectedGraph(String content) throws Exception {
        LOGGER.info("Parsing OARLIB content for UndirectedGraph...");
        
        String[] lines = content.split("\n");
        List<LinkData> links = new ArrayList<>();
        int numVertices = 0;
        int depotId = 1;
        
        boolean inLinksSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }
            
            if (line.contains("N:")) {
                try {
                    numVertices = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    // ignore
                }
                continue;
            }
            
            if (line.contains("Depot ID")) {
                try {
                    depotId = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    depotId = 1;
                }
                continue;
            }
            
            if (line.contains("LINKS") && !line.contains("END")) {
                inLinksSection = true;
                continue;
            }
            
            if (line.contains("END LINKS") || line.contains("VERTICES")) {
                inLinksSection = false;
                continue;
            }
            
            if (inLinksSection) {
                try {
                    LinkData linkData = parseLinkLine(line);
                    if (linkData != null) {
                        links.add(linkData);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse link: " + line);
                }
            }
        }
        
        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }
        
        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }
        
        LOGGER.info("Creating UndirectedGraph with " + numVertices + " vertices and " + links.size() + " edges");
        
        UndirectedGraph graph = new UndirectedGraph(numVertices);
        
        for (LinkData link : links) {
            try {
                graph.addEdge(link.v1, link.v2, "edge", link.cost, true);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link);
            }
        }
        
        try {
            graph.setDepotId(depotId);
        } catch (Exception e) {
            graph.setDepotId(1);
        }
        
        LOGGER.info("Graph parsing complete. Vertices: " + graph.getVertices().size() + 
                    ", Edges: " + graph.getEdges().size());
        
        return graph;
    }

    /**
     * Parse MixedGraph from OARLIB content string
     */
    public static MixedGraph parseMixedGraph(String content) throws Exception {
        LOGGER.info("Parsing OARLIB content for MixedGraph...");
        
        String[] lines = content.split("\n");
        List<LinkData> links = new ArrayList<>();
        int numVertices = 0;
        int depotId = 1;
        
        boolean inLinksSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }
            
            if (line.contains("N:")) {
                try {
                    numVertices = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    // ignore
                }
                continue;
            }
            
            if (line.contains("Depot ID")) {
                try {
                    depotId = Integer.parseInt(extractValue(line));
                } catch (Exception e) {
                    depotId = 1;
                }
                continue;
            }
            
            if (line.contains("LINKS") && !line.contains("END")) {
                inLinksSection = true;
                continue;
            }
            
            if (line.contains("END LINKS") || line.contains("VERTICES")) {
                inLinksSection = false;
                continue;
            }
            
            if (inLinksSection) {
                try {
                    LinkData linkData = parseLinkLine(line);
                    if (linkData != null) {
                        links.add(linkData);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse link: " + line);
                }
            }
        }
        
        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }
        
        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }
        
        LOGGER.info("Creating MixedGraph with " + numVertices + " vertices and " + links.size() + " edges");
        
        MixedGraph graph = new MixedGraph(numVertices);
        
        for (LinkData link : links) {
            try {
                // For mixed graphs, treat all edges as undirected by default
                graph.addEdge(link.v1, link.v2, "edge", link.cost, false);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link);
            }
        }
        
        try {
            graph.setDepotId(depotId);
        } catch (Exception e) {
            graph.setDepotId(1);
        }
        
        LOGGER.info("Graph parsing complete. Vertices: " + graph.getVertices().size() + 
                    ", Edges: " + graph.getEdges().size());
        
        return graph;
    }

    /**
     * Inner class to hold vertex data
     */
    static class VertexData {
        int id;
        double x, y;
        
        @Override
        public String toString() {
            return "VertexData{" +
                    "id=" + id +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}
