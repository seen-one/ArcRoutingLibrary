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
 * TeaVM-Wrapper - Enhanced wrapper for GeneralTestbed functionality
 * Supports OARLIB file parsing and solver execution in TeaVM/JavaScript
 */
public class TeaVMWrapper {

    private static final SimpleLogger LOGGER = SimpleLogger.getLogger(TeaVMWrapper.class);

    /**
     * Main entry point for TeaVM wrapper
     * Usage: teavm-wrapper [solver] [oarlib_content]
     * Mirrors GeneralTestbed behavior but parses input content string directly
     *
     * @param args [solver] [instance_content]
     */
    public static void main(String[] args) {
        if((args.length != 2 && !args[0].equals("7")) && !(args.length == 3 && args[0].equals("7"))) {
            for(String arg : args)
                System.out.println(arg);
            displayHelp();
            return;
        }

        try {
            int solver = Integer.parseInt(args[0]);
            String instanceContent = args[1];
            
            switch (solver) {
                case 1: //DCPP
                    try {
                        DirectedGraph dg = OARLibParser.parseDirectedGraph(instanceContent);
                        DirectedCPP dcpp = new DirectedCPP(dg, "instance");
                        DCPPSolver_Edmonds dcppSolver = new DCPPSolver_Edmonds(dcpp);
                        dcppSolver.trySolve();
                        System.out.println(dcppSolver.printCurrentSol());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case 2: //UCPP
                    try {
                        UndirectedGraph ug = OARLibParser.parseUndirectedGraph(instanceContent);
                        UndirectedCPP ucpp = new UndirectedCPP(ug, "instance");
                        UCPPSolver_Edmonds ucppSolver = new UCPPSolver_Edmonds(ucpp);
                        ucppSolver.trySolve();
                        System.out.println(ucppSolver.printCurrentSol());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case 3: //MCPP1
                    try {
                        MixedGraph mg = OARLibParser.parseMixedGraph(instanceContent);
                        MixedCPP mcpp = new MixedCPP(mg, "instance");
                        MCPPSolver_Frederickson mcppSolver = new MCPPSolver_Frederickson(mcpp);
                        mcppSolver.trySolve();
                        System.out.println(mcppSolver.printCurrentSol());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case 4: //MCPP2
                    try {
                        MixedGraph mg2 = OARLibParser.parseMixedGraph(instanceContent);
                        System.out.println("Vertex degrees:");
                        java.util.List<oarlib.vertex.impl.MixedVertex> vertices = new java.util.ArrayList<>(mg2.getVertices());
                        java.util.Collections.sort(vertices, (v1, v2) -> Integer.compare(v1.getId(), v2.getId()));
                        for(oarlib.vertex.impl.MixedVertex v : vertices) {
                            System.out.println("Vertex " + v.getId() + ": degree=" + v.getDegree() + ", inDegree=" + v.getInDegree() + ", outDegree=" + v.getOutDegree());
                        }
                        MixedCPP mcpp2 = new MixedCPP(mg2, "Instance");
                        MCPPSolver_Yaoyuenyong mcppSolver2 = new MCPPSolver_Yaoyuenyong(mcpp2);
                        mcppSolver2.trySolve();
                        System.out.println(mcppSolver2.printCurrentSol());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 5: //WPP
                    try {
                        WindyGraph wg = OARLibParser.parseWindyGraph(instanceContent);
                        WindyCPP wpp = new WindyCPP(wg, "Instance");
                        WRPPSolver_Win wppSolver = new WRPPSolver_Win(wpp);
                        wppSolver.trySolve();
                        System.out.println(wppSolver.printCurrentSol());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 6: //DRPP
                    System.out.println("ERROR: DRPP Solver is not supported in TeaVM due to native library dependencies (MSArbor).");
                    System.out.println("Please use solver 1, 2, 3, 4, 5, or 7 instead.");
                    break;
                case 7: //WRPP
                    try {
                        WindyGraph wg2 = OARLibParser.parseWindyGraph(instanceContent);
                        WindyRPP wrpp = new WindyRPP(wg2, "Instance");
                        WRPPSolver_Benavent_H1 wrppSolver = new WRPPSolver_Benavent_H1(wrpp);
                        wrppSolver.trySolve();
                        System.out.println(wrppSolver.printCurrentSol());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } catch (NumberFormatException ex) {
            displayHelp();
        }
    }

    /**
     * Display help information
     */
    private static void displayHelp(){
        String helpText = "===================";
        helpText += "\n";
        helpText += "\n";
        helpText += "Welcome to the Open Source Arc Routing Library (OARLib).\n";
        helpText += "If you would like to use this software as a command-line utility,\n";
        helpText += "please use the call structure: oarlib.jar [solver] [instance file path].\n\n";
        helpText += "[solver] - \n\n";
        helpText += "  1 - Directed Chinese Postman Exact Solver (Edmonds's Algorithm).\n";
        helpText += "  2 - Undirected Chinese Postman Exact Solver (Edmonds's Algorithm).\n";
        helpText += "  3 - Mixed Chinese Postman (Frederickson's Heuristic).\n";
        helpText += "  4 - Mixed Chinese Postman (Yaoyuenyong et al.'s Heuristic)\n";
        helpText += "  5 - Windy Chinese Postman (Win's Heuristic)\n";
        helpText += "  6 - Directed Rural Postman (Christofides's Heuristic)\n";
        helpText += "  7 - Windy Rural Postman (Benavent et al.'s Heuristic)\n";
        helpText += "\n";
        helpText += "If you would like to extend this code, or use its API directly,\n";
        helpText += "please see the README and docs for additional details.\n";
        helpText += "\n";
        helpText += "===================\n";

        System.out.println(helpText);
    }
}
