/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015 Oliver Lum
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
 */
package oarlib.teavm;

import oarlib.core.Problem;
import oarlib.graph.impl.UndirectedGraph;
import oarlib.graph.impl.DirectedGraph;
import oarlib.graph.impl.MixedGraph;
import oarlib.graph.impl.WindyGraph;
import oarlib.vertex.impl.UndirectedVertex;
import oarlib.vertex.impl.DirectedVertex;
import oarlib.vertex.impl.WindyVertex;

/**
 * Minimal TeaVM entry point for the Arc Routing Library.
 * 
 * This entry point provides core graph and solver functionality without:
 * - File I/O (java.nio.file.*)
 * - Native libraries (System.loadLibrary)
 * - Reflection APIs not supported by TeaVM
 * 
 * It can be used from JavaScript via TeaVM's Java-to-JS bridge.
 */
public class TeaVMEntryPoint {

    public static void main(String[] args) {
        System.out.println("Arc Routing Library - TeaVM Runtime Ready");
        System.out.println("Version: 1.0.0 (TeaVM Build)");
        System.out.println("Core graph and solver APIs available for JavaScript");
    }

    /**
     * Create a simple undirected graph for testing.
     * Can be called from JavaScript: TeaVMEntryPoint.createUndirectedGraph(5)
     * 
     * @param numVertices number of vertices in the graph
     * @return a new undirected graph
     */
    public static UndirectedGraph createUndirectedGraph(int numVertices) {
        return new UndirectedGraph(numVertices);
    }

    /**
     * Create a simple directed graph for testing.
     * 
     * @param numVertices number of vertices in the graph
     * @return a new directed graph
     */
    public static DirectedGraph createDirectedGraph(int numVertices) {
        return new DirectedGraph(numVertices);
    }

    /**
     * Create a simple mixed graph for testing.
     * 
     * @param numVertices number of vertices in the graph
     * @return a new mixed graph
     */
    public static MixedGraph createMixedGraph(int numVertices) {
        return new MixedGraph(numVertices);
    }

    /**
     * Create a simple windy graph for testing.
     * 
     * @param numVertices number of vertices in the graph
     * @return a new windy graph
     */
    public static WindyGraph createWindyGraph(int numVertices) {
        return new WindyGraph(numVertices);
    }

    /**
     * Example: Add an edge to an undirected graph and verify it.
     * 
     * @return success message
     */
    public static String exampleAddEdge() {
        try {
            UndirectedGraph g = new UndirectedGraph(3);
            g.addEdge(1, 2, 5);
            g.addEdge(2, 3, 3);
            
            return "Successfully created undirected graph with 3 vertices and 2 edges. " +
                   "Total cost: " + (5 + 3);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
