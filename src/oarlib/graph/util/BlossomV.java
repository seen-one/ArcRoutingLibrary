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
package oarlib.graph.util;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedPerfectMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.Set;

public class BlossomV {

    /**
     * Minimum-cost perfect matching algorithm using JGraphT's Kolmogorov Blossom V implementation.
     * This provides an optimal solution for the minimum-cost perfect matching problem.
     *
     * @param n       - num nodes
     * @param m       - num edges
     * @param edges   - edge i connects vertices edges[2i] and edges[2i+1]
     * @param weights - edge i has cost weights[i]
     * @return - matching array where result[i] is the vertex matched to vertex i
     */
    public static int[] blossomV(int n, int m, int[] edges, int[] weights) {
        // Create a weighted graph using JGraphT
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        
        // Add all vertices (0 to n-1)
        for (int i = 0; i < n; i++) {
            graph.addVertex(i);
        }
        
        // Add all weighted edges
        for (int i = 0; i < m; i++) {
            int u = edges[2 * i];
            int v = edges[2 * i + 1];
            double weight = weights[i];
            
            DefaultWeightedEdge edge = graph.addEdge(u, v);
            if (edge != null) {
                graph.setEdgeWeight(edge, weight);
            }
        }
        
        // Run Kolmogorov's Blossom V algorithm
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> matcher = 
            new KolmogorovWeightedPerfectMatching<>(graph);
        
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching = matcher.getMatching();
        
        // Convert the matching result to the expected format
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = -1; // Initialize as unmatched
        }
        
        // Extract matched pairs from JGraphT result
        Set<DefaultWeightedEdge> matchedEdges = matching.getEdges();
        for (DefaultWeightedEdge edge : matchedEdges) {
            int u = graph.getEdgeSource(edge);
            int v = graph.getEdgeTarget(edge);
            result[u] = v;
            result[v] = u;
        }
        
        return result;
    }

}
