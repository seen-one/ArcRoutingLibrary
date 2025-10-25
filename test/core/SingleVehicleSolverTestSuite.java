package core;

import oarlib.util.SimpleLogger;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
// import gurobi.*; // Gurobi is optional and not available in all environments
import oarlib.core.Route;
import oarlib.graph.graphgen.erdosrenyi.DirectedErdosRenyiGraphGenerator;
import oarlib.graph.graphgen.erdosrenyi.UndirectedErdosRenyiGraphGenerator;
import oarlib.graph.impl.DirectedGraph;
import oarlib.graph.impl.UndirectedGraph;
import oarlib.graph.util.CommonAlgorithms;
import oarlib.link.impl.Arc;
import oarlib.link.impl.Edge;
import oarlib.problem.impl.cpp.DirectedCPP;
import oarlib.problem.impl.cpp.UndirectedCPP;
import oarlib.solver.impl.DCPPSolver_Edmonds;
import oarlib.solver.impl.UCPPSolver_Edmonds;
import oarlib.vertex.impl.DirectedVertex;
import oarlib.vertex.impl.UndirectedVertex;
import org.junit.Test;

import java.util.ArrayList;


/**
 * Suite of unit tests to verify the functionality of the single vehicle solvers.
 * <p/>
 * Created by oliverlum on 11/11/14.
 */
public class SingleVehicleSolverTestSuite {

    private static final SimpleLogger LOGGER = SimpleLogger.getLogger(SingleVehicleSolverTestSuite.class);

    @Test
    public void testUCPPSolver() {
        // Gurobi dependency removed - this test requires optional Gurobi solver
        // Skipping test
        LOGGER.info("testUCPPSolver skipped - requires optional Gurobi dependency");
    }

    @Test
    public void testDCPPSolver() {
        // Gurobi dependency removed - this test requires optional Gurobi solver
        // Skipping test
        LOGGER.info("testDCPPSolver skipped - requires optional Gurobi dependency");
    }

    @Test
    public void testFredericksonMCPPSolver() {

        //read in the instances

        //compare a few


    }

    @Test
    public void testYaoyuenyongMCPPSolver() {

        //read in the instances

        //compare a few

    }

    @Test
    public void testWinWRPPSolver() {


        //read in the instances

        //read in the exact solutions

        //compare the average +- delta


    }

    @Test
    public void testBenaventWRPPSolver() {

        //read in the instances

        //read in the exact solutions

        //compare the average +- delta

    }
}
