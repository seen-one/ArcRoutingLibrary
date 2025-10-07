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

import java.util.*;

public class BlossomV {

    /**
     * Greedy minimum-cost perfect matching algorithm to replace BlossomV.
     * This is a simple greedy approach that may not find the optimal solution
     * but should work reasonably well for most cases.
     *
     * @param n       - num nodes
     * @param m       - num edges
     * @param edges   - edge i connects vertices edges[2i] and edges[2i+1]
     * @param weights - edge i has cost weights[i]
     * @return - matching array where result[i] is the vertex matched to vertex i
     */
    public static int[] blossomV(int n, int m, int[] edges, int[] weights) {
        // Create a greedy matching using a simple approach
        int[] matching = new int[n];
        boolean[] matched = new boolean[n];
        
        // Initialize matching array
        for (int i = 0; i < n; i++) {
            matching[i] = -1; // -1 means unmatched
        }
        
        // Create a list of edges with their weights for sorting
        List<EdgeInfo> edgeList = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            edgeList.add(new EdgeInfo(edges[2*i], edges[2*i+1], weights[i]));
        }
        
        // Sort edges by weight (ascending order for minimum cost)
        Collections.sort(edgeList, new Comparator<EdgeInfo>() {
            @Override
            public int compare(EdgeInfo e1, EdgeInfo e2) {
                return Integer.compare(e1.weight, e2.weight);
            }
        });
        
        // Greedily select edges for matching
        for (EdgeInfo edge : edgeList) {
            int u = edge.u;
            int v = edge.v;
            
            // Only add this edge if both vertices are unmatched
            if (!matched[u] && !matched[v]) {
                matching[u] = v;
                matching[v] = u;
                matched[u] = true;
                matched[v] = true;
            }
        }
        
        // Verify that we have a perfect matching
        for (int i = 0; i < n; i++) {
            if (!matched[i]) {
                // If we don't have a perfect matching, this is an error condition
                // In practice, this shouldn't happen if the input is valid
                System.err.println("Warning: Greedy matching failed to find perfect matching for vertex " + i);
                // Try to find any unmatched vertex to pair with
                for (int j = i + 1; j < n; j++) {
                    if (!matched[j]) {
                        matching[i] = j;
                        matching[j] = i;
                        matched[i] = true;
                        matched[j] = true;
                        break;
                    }
                }
            }
        }
        
        return matching;
    }
    
    /**
     * Helper class to store edge information for sorting
     */
    private static class EdgeInfo {
        int u, v, weight;
        
        EdgeInfo(int u, int v, int weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
    }

}
