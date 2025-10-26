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

import oarlib.util.SimpleLogger;
import oarlib.core.Graph;
import oarlib.core.Route;
import oarlib.graph.impl.*;
import oarlib.link.impl.*;
import oarlib.problem.impl.cpp.DirectedCPP;
import oarlib.problem.impl.cpp.MixedCPP;
import oarlib.problem.impl.cpp.UndirectedCPP;
import oarlib.problem.impl.cpp.WindyCPP;
import oarlib.problem.impl.rpp.DirectedRPP;
import oarlib.problem.impl.rpp.WindyRPP;
import oarlib.solver.impl.*;
import oarlib.vertex.impl.MixedVertex;

import java.util.*;

/**
 * TeaVM Entry Point - A web-compatible version of GeneralTestbed
 * Uses string-based graph definitions instead of file I/O for TeaVM compatibility
 */
public class TeaVMEntryPoint {

    private static final SimpleLogger LOGGER = SimpleLogger.getLogger(TeaVMEntryPoint.class);

    /**
     * Main entry point for TeaVM compilation.
     * Takes a solver type and instance name (as strings, no file I/O)
     * 
     * @param args [solver] [instanceName]
     */
    public static void main(String[] args) {
        // TeaVM version with string-based test data
        if((args.length != 2 && !args[0].equals("7")) && !(args.length == 3 && args[0].equals("7"))) {
            displayHelp();
            return;
        }

        try {
            int solver = Integer.parseInt(args[0]);
            String instanceName = args[1];
            
            switch (solver) {
                case 1: // DCPP
                    solveDCPP(instanceName);
                    break;
                case 2: // UCPP
                    solveUCPP(instanceName);
                    break;
                case 3: // MCPP1
                    solveMCPP1(instanceName);
                    break;
                case 4: // MCPP2
                    solveMCPP2(instanceName);
                    break;
                case 5: // WPP
                    solveWPP(instanceName);
                    break;
                case 6: // DRPP
                    solveDRPP(instanceName);
                    break;
                case 7: // WRPP
                    solveWRPP(instanceName);
                    break;
                default:
                    displayHelp();
            }
        } catch (NumberFormatException ex) {
            displayHelp();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Solves a Directed Chinese Postman Problem using Edmonds's Algorithm
     */
    private static void solveDCPP(String instanceName) {
        try {
            DirectedGraph dg = createTestDirectedGraph(instanceName);
            DirectedCPP dcpp = new DirectedCPP(dg, instanceName);
            DCPPSolver_Edmonds dcppSolver = new DCPPSolver_Edmonds(dcpp);
            dcppSolver.trySolve();
            System.out.println(dcppSolver.printCurrentSol());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Solves an Undirected Chinese Postman Problem using Edmonds's Algorithm
     */
    private static void solveUCPP(String instanceName) {
        try {
            UndirectedGraph ug = createTestUndirectedGraph(instanceName);
            UndirectedCPP ucpp = new UndirectedCPP(ug, instanceName);
            UCPPSolver_Edmonds ucppSolver = new UCPPSolver_Edmonds(ucpp);
            ucppSolver.trySolve();
            System.out.println(ucppSolver.printCurrentSol());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Solves a Mixed Chinese Postman Problem (Frederickson's Heuristic)
     */
    private static void solveMCPP1(String instanceName) {
        try {
            MixedGraph mg = createTestMixedGraph(instanceName);
            MixedCPP mcpp = new MixedCPP(mg, instanceName);
            MCPPSolver_Frederickson mcppSolver = new MCPPSolver_Frederickson(mcpp);
            mcppSolver.trySolve();
            System.out.println(mcppSolver.printCurrentSol());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Solves a Mixed Chinese Postman Problem (Yaoyuenyong's Heuristic)
     */
    private static void solveMCPP2(String instanceName) {
        try {
            MixedGraph mg = createTestMixedGraph(instanceName);
            
            // Print degrees
            System.out.println("Vertex degrees:");
            List<MixedVertex> vertices = new ArrayList<>(mg.getVertices());
            Collections.sort(vertices, (v1, v2) -> Integer.compare(v1.getId(), v2.getId()));
            for(MixedVertex v : vertices) {
                System.out.println("Vertex " + v.getId() + ": degree=" + v.getDegree() + 
                    ", inDegree=" + v.getInDegree() + ", outDegree=" + v.getOutDegree());
            }
            
            MixedCPP mcpp = new MixedCPP(mg, instanceName);
            MCPPSolver_Yaoyuenyong mcppSolver = new MCPPSolver_Yaoyuenyong(mcpp);
            mcppSolver.trySolve();
            System.out.println(mcppSolver.printCurrentSol());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Solves a Windy Postman Problem using Win's Heuristic
     */
    private static void solveWPP(String instanceName) {
        try {
            WindyGraph wg = createTestWindyGraph(instanceName);
            WindyCPP wpp = new WindyCPP(wg, instanceName);
            WRPPSolver_Win wppSolver = new WRPPSolver_Win(wpp);
            wppSolver.trySolve();
            System.out.println(wppSolver.printCurrentSol());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Solves a Directed Rural Postman Problem using Christofides's Heuristic
     * Note: Not supported in TeaVM due to MSArbor's native library dependency
     */
    private static void solveDRPP(String instanceName) {
        try {
            System.out.println("Error: DRPP Solver (case 6) is not supported in TeaVM.");
            System.out.println("Reason: DRPPSolver_Christofides requires MSArbor which depends on native libraries.");
            System.out.println("Please use a different solver or run on the JVM version.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Solves a Windy Rural Postman Problem using Benavent's Heuristic
     */
    private static void solveWRPP(String instanceName) {
        try {
            WindyGraph wg = createTestWindyGraph(instanceName);
            WindyRPP wrpp = new WindyRPP(wg, instanceName);
            WRPPSolver_Benavent_H1 wrppSolver = new WRPPSolver_Benavent_H1(wrpp);
            wrppSolver.trySolve();
            System.out.println(wrppSolver.printCurrentSol());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a test directed graph based on instance name
     */
    private static DirectedGraph createTestDirectedGraph(String instanceName) {
        try {
            DirectedGraph dg = new DirectedGraph(4);
            dg.addEdge(1, 2, "test", 5, true);
            dg.addEdge(2, 3, "test", 3, true);
            dg.addEdge(3, 4, "test", 7, true);
            dg.addEdge(4, 1, "test", 2, true);
            dg.setDepotId(1);
            return dg;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a test undirected graph based on instance name
     */
    private static UndirectedGraph createTestUndirectedGraph(String instanceName) {
        try {
            UndirectedGraph ug = new UndirectedGraph(4);
            ug.addEdge(1, 2, "test", 5, true);
            ug.addEdge(2, 3, "test", 3, true);
            ug.addEdge(3, 4, "test", 7, true);
            ug.addEdge(4, 1, "test", 2, true);
            ug.setDepotId(1);
            return ug;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a test mixed graph based on instance name
     */
    private static MixedGraph createTestMixedGraph(String instanceName) {
        try {
            MixedGraph mg = new MixedGraph(5);
            mg.addEdge(1, 2, "test", 4, true);      // directed
            mg.addEdge(2, 3, "test", 5, false);     // undirected
            mg.addEdge(3, 4, "test", 3, true);      // directed
            mg.addEdge(4, 5, "test", 6, false);     // undirected
            mg.addEdge(5, 1, "test", 2, true);      // directed
            mg.setDepotId(1);
            return mg;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a test windy graph based on instance name
     */
    private static WindyGraph createTestWindyGraph(String instanceName) {
        try {
            WindyGraph wg = new WindyGraph(6);
            wg.addEdge(1, 2, 5, 7, false);  // forward cost, reverse cost, required
            wg.addEdge(2, 3, 4, 6, false);
            wg.addEdge(3, 4, 3, 5, false);
            wg.addEdge(4, 5, 7, 9, false);
            wg.addEdge(5, 6, 2, 4, false);
            wg.addEdge(6, 1, 6, 8, false);
            wg.setDepotId(1);
            return wg;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Display help text
     */
    private static void displayHelp(){
        String helpText = "===================";
        helpText += "\n";
        helpText += "\n";
        helpText += "Welcome to the TeaVM Arc Routing Solver.\n";
        helpText += "TeaVM version - uses string-based test data instead of file I/O.\n";
        helpText += "Usage: teavm [solver] [instance_name]\n\n";
        helpText += "[solver] - \n\n";
        helpText += "  1 - Directed Chinese Postman (Edmonds).\n";
        helpText += "  2 - Undirected Chinese Postman (Edmonds).\n";
        helpText += "  3 - Mixed Chinese Postman (Frederickson).\n";
        helpText += "  4 - Mixed Chinese Postman (Yaoyuenyong).\n";
        helpText += "  5 - Windy Chinese Postman (Win).\n";
        helpText += "  6 - [NOT SUPPORTED] Directed Rural Postman (requires native library).\n";
        helpText += "  7 - Windy Rural Postman (Benavent H1).\n";
        helpText += "\n";
        helpText += "===================\n";

        System.out.println(helpText);
    }
}
