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
 * Supports Windy, Mixed, Directed, and Undirected graph formats.
 */
public class OARLibParser {

    private static final SimpleLogger LOGGER = SimpleLogger.getLogger(OARLibParser.class);

    private static final int DEFAULT_FALLBACK_VERTEX_COUNT = 150;

    private enum LinkFormatType {
        WINDY,
        MIXED,
        DIRECTED,
        UNDIRECTED,
        UNKNOWN
    }

    /**
     * Parse OARLIB format string and create a WindyGraph
     *
     * @param content The full OARLIB file content as a string
     * @return A WindyGraph instance populated from the file content
     * @throws Exception if parsing fails
     */
    public static WindyGraph parseWindyGraph(String content) throws Exception {
        LOGGER.info("Parsing OARLIB content...");

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("OARLIB content is empty.");
        }

        String normalizedContent = sanitizeContent(content);
        String[] lines = normalizedContent.split("\n");
        List<LinkData> links = new ArrayList<>();
        List<VertexData> vertices = new ArrayList<>();

        boolean inLinksSection = false;
        boolean inVerticesSection = false;
        int numVertices = 0;
        int depotId = 1;
        LinkFormatType linkFormat = LinkFormatType.WINDY;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }

            if (line.contains("Depot ID")) {
                Integer depot = parseIntSafe(extractValue(line));
                if (depot != null) {
                    depotId = depot;
                }
                continue;
            }

            if (line.contains("N:")) {
                Integer declaredVertices = parseIntSafe(extractValue(line));
                if (declaredVertices != null) {
                    numVertices = declaredVertices;
                }
                continue;
            }

            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.startsWith("LINKS") && !upper.contains("END")) {
                inLinksSection = true;
                inVerticesSection = false;
                linkFormat = LinkFormatType.WINDY;
                continue;
            }

            if (upper.contains("END LINKS")) {
                inLinksSection = false;
                continue;
            }

            if (upper.startsWith("VERTICES") && !upper.contains("END")) {
                inVerticesSection = true;
                inLinksSection = false;
                continue;
            }

            if (upper.contains("END VERTICES")) {
                inVerticesSection = false;
                continue;
            }

            if (inLinksSection) {
                if (upper.startsWith("LINE FORMAT")) {
                    linkFormat = resolveLinkFormat(extractValue(line), LinkFormatType.WINDY);
                    continue;
                }

                LinkData linkData = parseLinkLine(line, linkFormat);
                if (linkData != null) {
                    links.add(linkData);
                } else {
                    LOGGER.warn("Skipping malformed link line: " + line);
                }
                continue;
            }

            if (inVerticesSection) {
                if (upper.startsWith("LINE FORMAT")) {
                    continue;
                }

                VertexData vertexData = parseVertexLine(line);
                if (vertexData != null) {
                    if (vertexData.id <= 0) {
                        vertexData.id = vertices.size() + 1;
                    }
                    vertices.add(vertexData);
                } else {
                    LOGGER.warn("Skipping malformed vertex line: " + line);
                }
            }
        }

        numVertices = inferVertexCount(numVertices, links, vertices);

        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }

        if (numVertices <= 0) {
            numVertices = DEFAULT_FALLBACK_VERTEX_COUNT;
        }

        LOGGER.info("Creating WindyGraph with " + numVertices + " vertices and " + links.size() + " edges");

        WindyGraph graph = new WindyGraph(numVertices);

        for (LinkData link : links) {
            int from = link.v1;
            int to = link.v2;
            int cost = link.cost;
            int reverseCost = (link.reverseCost != null) ? link.reverseCost : cost;
            boolean required = link.isRequired != null ? link.isRequired : true;

            try {
                graph.addEdge(from, to, cost, reverseCost, required);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link + "; reason: " + e.getMessage());
            }
        }

        for (VertexData vertex : vertices) {
            if (vertex.id > 0 && vertex.id <= graph.getVertices().size()) {
                try {
                    graph.getVertex(vertex.id).setCoordinates(vertex.x, vertex.y);
                } catch (Exception e) {
                    LOGGER.warn("Failed to set vertex coordinates: " + vertex + "; reason: " + e.getMessage());
                }
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
     * Parse WindyGraph from content with format auto-detection.
     * Tries Corberan format first, then falls back to OARLib format.
     * This mirrors the behavior of GeneralTestbed.java for WRPP solver.
     *
     * @param content The full file content as a string
     * @return A WindyGraph instance populated from the file content
     * @throws Exception if parsing fails
     */
    public static WindyGraph parseWindyGraphWithFormatDetection(String content) throws Exception {
        LOGGER.info("Parsing WindyGraph with format auto-detection...");

        // Try Corberan format first
        try {
            LOGGER.info("Attempting to parse as Corberan format...");
            return parseWindyGraphCorberan(content);
        } catch (Exception e) {
            LOGGER.info("Could not read file in Corberan format; attempting to read in OARLib format.");
            // Fall back to OARLib format
            try {
                return parseWindyGraph(content);
            } catch (Exception e2) {
                throw new Exception("Failed to parse WindyGraph in both Corberan and OARLib formats", e2);
            }
        }
    }

    /**
     * Parse WindyGraph from Corberan format content string.
     * Corberan format is similar to OARLib but may have slightly different structure.
     *
     * @param content The full Corberan file content as a string
     * @return A WindyGraph instance populated from the file content
     * @throws Exception if parsing fails
     */
    public static WindyGraph parseWindyGraphCorberan(String content) throws Exception {
        LOGGER.info("Parsing Corberan format content...");

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Corberan content is empty.");
        }

        // For now, Corberan and OARLib formats are similar enough that we can parse them the same way
        // The main difference is in how they detect the format. If this throws an exception,
        // it will be caught by the caller and OARLib format will be tried.
        
        String normalizedContent = sanitizeContent(content);
        
        // Check for Corberan-specific markers
        if (!normalizedContent.toLowerCase().contains("corberan") && 
            !normalizedContent.contains("Corberan")) {
            // If no Corberan marker found, this is likely not Corberan format
            throw new IllegalArgumentException("Content does not appear to be in Corberan format");
        }

        // Parse using same logic as OARLib (they're compatible)
        // Re-use the parseWindyGraph logic but let it fail if format is wrong
        return parseWindyGraph(content);
    }

    /**
     * Parse DirectedGraph from OARLIB content string
     */
    public static DirectedGraph parseDirectedGraph(String content) throws Exception {
        LOGGER.info("Parsing OARLIB content for DirectedGraph...");

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("OARLIB content is empty.");
        }

        String normalizedContent = sanitizeContent(content);
        String[] lines = normalizedContent.split("\n");
        List<LinkData> links = new ArrayList<>();
        int numVertices = 0;
        int depotId = 1;
        boolean inLinksSection = false;
        LinkFormatType linkFormat = LinkFormatType.DIRECTED;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }

            if (line.contains("N:")) {
                Integer declaredVertices = parseIntSafe(extractValue(line));
                if (declaredVertices != null) {
                    numVertices = declaredVertices;
                }
                continue;
            }

            if (line.contains("Depot ID")) {
                Integer depot = parseIntSafe(extractValue(line));
                if (depot != null) {
                    depotId = depot;
                }
                continue;
            }

            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.startsWith("LINKS") && !upper.contains("END")) {
                inLinksSection = true;
                linkFormat = LinkFormatType.DIRECTED;
                continue;
            }

            if (upper.contains("END LINKS") || upper.contains("VERTICES")) {
                inLinksSection = false;
                if (upper.contains("VERTICES")) {
                    continue;
                }
            }

            if (inLinksSection) {
                if (upper.startsWith("LINE FORMAT")) {
                    linkFormat = resolveLinkFormat(extractValue(line), LinkFormatType.DIRECTED);
                    continue;
                }

                LinkData linkData = parseLinkLine(line, linkFormat);
                if (linkData != null) {
                    links.add(linkData);
                } else {
                    LOGGER.warn("Skipping malformed link line: " + line);
                }
            }
        }

        numVertices = inferVertexCount(numVertices, links, null);

        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }

        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }

        LOGGER.info("Creating DirectedGraph with " + numVertices + " vertices and " + links.size() + " edges");

        DirectedGraph graph = new DirectedGraph(numVertices);

        for (LinkData link : links) {
            boolean required = link.isRequired != null ? link.isRequired : true;
            try {
                graph.addEdge(link.v1, link.v2, "edge", link.cost, required);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link + "; reason: " + e.getMessage());
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

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("OARLIB content is empty.");
        }

        String normalizedContent = sanitizeContent(content);
        String[] lines = normalizedContent.split("\n");
        List<LinkData> links = new ArrayList<>();
        int numVertices = 0;
        int depotId = 1;
        boolean inLinksSection = false;
        LinkFormatType linkFormat = LinkFormatType.UNDIRECTED;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }

            if (line.contains("N:")) {
                Integer declaredVertices = parseIntSafe(extractValue(line));
                if (declaredVertices != null) {
                    numVertices = declaredVertices;
                }
                continue;
            }

            if (line.contains("Depot ID")) {
                Integer depot = parseIntSafe(extractValue(line));
                if (depot != null) {
                    depotId = depot;
                }
                continue;
            }

            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.startsWith("LINKS") && !upper.contains("END")) {
                inLinksSection = true;
                linkFormat = LinkFormatType.UNDIRECTED;
                continue;
            }

            if (upper.contains("END LINKS") || upper.contains("VERTICES")) {
                inLinksSection = false;
                continue;
            }

            if (inLinksSection) {
                if (upper.startsWith("LINE FORMAT")) {
                    linkFormat = resolveLinkFormat(extractValue(line), LinkFormatType.UNDIRECTED);
                    continue;
                }

                LinkData linkData = parseLinkLine(line, linkFormat);
                if (linkData != null) {
                    links.add(linkData);
                } else {
                    LOGGER.warn("Skipping malformed link line: " + line);
                }
            }
        }

        numVertices = inferVertexCount(numVertices, links, null);

        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }

        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }

        LOGGER.info("Creating UndirectedGraph with " + numVertices + " vertices and " + links.size() + " edges");

        UndirectedGraph graph = new UndirectedGraph(numVertices);

        for (LinkData link : links) {
            boolean required = link.isRequired != null ? link.isRequired : true;
            try {
                graph.addEdge(link.v1, link.v2, "edge", link.cost, required);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link + "; reason: " + e.getMessage());
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

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("OARLIB content is empty.");
        }

        String normalizedContent = sanitizeContent(content);
        String[] lines = normalizedContent.split("\n");
        List<LinkData> links = new ArrayList<>();
        int numVertices = 0;
        int depotId = 1;
        boolean inLinksSection = false;
        LinkFormatType linkFormat = LinkFormatType.MIXED;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }

            if (line.contains("N:")) {
                Integer declaredVertices = parseIntSafe(extractValue(line));
                if (declaredVertices != null) {
                    numVertices = declaredVertices;
                }
                continue;
            }

            if (line.contains("Depot ID")) {
                Integer depot = parseIntSafe(extractValue(line));
                if (depot != null) {
                    depotId = depot;
                }
                continue;
            }

            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.startsWith("LINKS") && !upper.contains("END")) {
                inLinksSection = true;
                linkFormat = LinkFormatType.MIXED;
                continue;
            }

            if (upper.contains("END LINKS") || upper.contains("VERTICES")) {
                inLinksSection = false;
                continue;
            }

            if (inLinksSection) {
                if (upper.startsWith("LINE FORMAT")) {
                    linkFormat = resolveLinkFormat(extractValue(line), LinkFormatType.MIXED);
                    continue;
                }

                LinkData linkData = parseLinkLine(line, linkFormat);
                if (linkData != null) {
                    links.add(linkData);
                } else {
                    LOGGER.warn("Skipping malformed link line: " + line);
                }
            }
        }

        numVertices = inferVertexCount(numVertices, links, null);

        if (links.isEmpty()) {
            throw new IllegalArgumentException("No valid LINKS section found in OARLIB content.");
        }

        if (numVertices <= 0) {
            throw new IllegalArgumentException("Unable to determine vertex count from OARLIB content.");
        }

        LOGGER.info("Creating MixedGraph with " + numVertices + " vertices and " + links.size() + " edges");

        MixedGraph graph = new MixedGraph(numVertices);

        for (LinkData link : links) {
            boolean isDirected = link.isDirected != null ? link.isDirected : false;
            boolean required = link.isRequired != null ? link.isRequired : true;
            try {
                graph.addEdge(link.v1, link.v2, link.cost, isDirected, required);
            } catch (Exception e) {
                LOGGER.warn("Failed to add edge: " + link + "; reason: " + e.getMessage());
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

    /**
     * Inner class to hold link data
     */
    static class LinkData {
        Integer v1;
        Integer v2;
        Integer cost;
        Integer reverseCost;
        Boolean isDirected;
        Boolean isRequired;

        @Override
        public String toString() {
            return "LinkData{" +
                    "v1=" + v1 +
                    ", v2=" + v2 +
                    ", cost=" + cost +
                    ", reverseCost=" + reverseCost +
                    ", isDirected=" + isDirected +
                    ", isRequired=" + isRequired +
                    '}';
        }
    }

    private static String sanitizeContent(String content) {
        String normalized = content;
        if (normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1);
        }
        normalized = normalized.replace("\r\n", "\n").replace("\r", "\n");
        return normalized;
    }

    private static LinkFormatType resolveLinkFormat(String formatSpec, LinkFormatType fallback) {
        if (formatSpec == null) {
            return fallback;
        }

        String normalized = formatSpec.toUpperCase(Locale.ROOT);

        if (normalized.contains("ISDIRECTED")) {
            return LinkFormatType.MIXED;
        }

        if (normalized.contains("REVERSE")) {
            return LinkFormatType.WINDY;
        }

        if (normalized.contains("ISREQUIRED") || normalized.contains("REQUIRED")) {
            if (fallback == LinkFormatType.DIRECTED) {
                return LinkFormatType.DIRECTED;
            }
            if (fallback == LinkFormatType.UNDIRECTED) {
                return LinkFormatType.UNDIRECTED;
            }
        }

        return fallback;
    }

    private static LinkData parseLinkLine(String line, LinkFormatType formatType) {
        String[] rawParts = line.split(",");
        List<String> parts = new ArrayList<>();
        for (String rawPart : rawParts) {
            String trimmed = rawPart.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }

        if (parts.size() < 3) {
            return null;
        }

        LinkData data = new LinkData();
        data.v1 = parseIntSafe(parts.get(0));
        data.v2 = parseIntSafe(parts.get(1));
        data.cost = parseIntSafe(parts.get(2));

        if (data.v1 == null || data.v2 == null || data.cost == null) {
            return null;
        }

        switch (formatType) {
            case MIXED:
                if (parts.size() >= 4) {
                    data.isDirected = parseBooleanToken(parts.get(3));
                }
                if (parts.size() >= 5) {
                    data.isRequired = parseBooleanToken(parts.get(4));
                }
                break;
            case DIRECTED:
            case UNDIRECTED:
                if (parts.size() >= 4) {
                    data.isRequired = parseBooleanToken(parts.get(3));
                }
                break;
            case WINDY:
            case UNKNOWN:
            default:
                if (parts.size() >= 4) {
                    data.reverseCost = parseIntSafe(parts.get(3));
                }
                if (parts.size() >= 5) {
                    data.isRequired = parseBooleanToken(parts.get(4));
                }
                break;
        }

        return data;
    }

    private static VertexData parseVertexLine(String line) {
        String[] rawParts = line.split(",");
        if (rawParts.length < 2) {
            return null;
        }

        VertexData data = new VertexData();
        int coordinateIndex = 0;

        if (rawParts.length >= 3) {
            Integer maybeId = parseIntSafe(rawParts[0]);
            if (maybeId != null) {
                data.id = maybeId;
                coordinateIndex = 1;
            }
        }

        try {
            data.x = Double.parseDouble(rawParts[coordinateIndex].trim());
            data.y = Double.parseDouble(rawParts[coordinateIndex + 1].trim());
        } catch (Exception e) {
            return null;
        }

        return data;
    }

    private static Integer parseIntSafe(String token) {
        if (token == null) {
            return null;
        }

        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBooleanToken(String token) {
        if (token == null) {
            return null;
        }

        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("t") ||
                trimmed.equalsIgnoreCase("yes") || trimmed.equalsIgnoreCase("y")) {
            return Boolean.TRUE;
        }

        if (trimmed.equalsIgnoreCase("false") || trimmed.equalsIgnoreCase("f") ||
                trimmed.equalsIgnoreCase("no") || trimmed.equalsIgnoreCase("n")) {
            return Boolean.FALSE;
        }

        if (trimmed.equals("1")) {
            return Boolean.TRUE;
        }

        if (trimmed.equals("0")) {
            return Boolean.FALSE;
        }

        try {
            return Integer.parseInt(trimmed) != 0;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractValue(String line) {
        int idx = line.indexOf(":");
        if (idx >= 0 && idx < line.length() - 1) {
            return line.substring(idx + 1).trim();
        }
        return "";
    }

    private static int inferVertexCount(int declaredCount, List<LinkData> links, List<VertexData> vertices) {
        int maxId = 0;

        if (links != null) {
            for (LinkData link : links) {
                if (link.v1 != null) {
                    maxId = Math.max(maxId, link.v1);
                }
                if (link.v2 != null) {
                    maxId = Math.max(maxId, link.v2);
                }
            }
        }

        if (vertices != null) {
            for (VertexData vertex : vertices) {
                if (vertex != null && vertex.id > 0) {
                    maxId = Math.max(maxId, vertex.id);
                }
            }
        }

        if (declaredCount > 0 && declaredCount >= maxId) {
            return declaredCount;
        }

        return maxId == 0 ? declaredCount : maxId;
    }
}
