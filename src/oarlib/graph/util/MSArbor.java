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

public class MSArbor {

    /**
     * The actual JNI call to the shortest spanning arborescence C++ code MSArbor.
     * We are following in the spirit of the msa15 example included with the code,
     * and providing directly the edge weights instead of extracting them from a
     * formatted text file.
     *
     * This method is stubbed for TeaVM compatibility (no native library support).
     * Callers should check for null return value.
     *
     * @param n       - the number of nodes
     * @param m       - the number of edges
     * @param weights - the associated edge costs; the graph is assumed to be complete, and
     *                the weights are ordered according to the source vertex
     * @return an array of spanning arborescence edges, or null if not available
     */
    public static int[] msArbor(int n, int m, int[] weights) {
        // Stub implementation for TeaVM and environments without native library
        // Simply return null - callers must handle gracefully
        return null;
    }

    static {
        // Attempt to load native library only in JVM environments
        // Silently ignore failures for TeaVM compatibility
        if (!isTeaVM()) {
            try {
                System.loadLibrary("MSArbor");
            } catch (Throwable t) {
                // Native library not available - will use stub implementation
            }
        }
    }

    /**
     * Check if running under TeaVM (simple heuristic).
     * @return true if likely running under TeaVM
     */
    private static boolean isTeaVM() {
        try {
            // TeaVM doesn't have java.nio.file
            Class.forName("java.nio.file.Paths");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
