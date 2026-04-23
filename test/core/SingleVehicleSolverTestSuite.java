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
import oarlib.graph.impl.WindyGraph;
import oarlib.graph.util.CommonAlgorithms;
import oarlib.link.impl.Arc;
import oarlib.link.impl.Edge;
import oarlib.problem.impl.cpp.WindyCPP;
import oarlib.problem.impl.cpp.DirectedCPP;
import oarlib.problem.impl.cpp.UndirectedCPP;
import oarlib.problem.impl.io.ProblemFormat;
import oarlib.problem.impl.io.ProblemReader;
import oarlib.solver.impl.DCPPSolver_Edmonds;
import oarlib.solver.impl.WRPPSolver_Win;
import oarlib.solver.impl.UCPPSolver_Edmonds;
import oarlib.vertex.impl.DirectedVertex;
import oarlib.vertex.impl.UndirectedVertex;
import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


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
    public void testDCPPSolverLongRouteCost() throws Exception {
        DirectedGraph graph = new DirectedGraph(3);
        graph.addEdge(1, 2, 1000000000L, true);
        graph.addEdge(2, 3, 1000000000L, true);
        graph.addEdge(3, 1, 1000000000L, true);
        graph.addEdge(1, 3, 1000000000L, true);

        DirectedCPP problem = new DirectedCPP(graph, "long-cost-regression");
        DCPPSolver_Edmonds solver = new DCPPSolver_Edmonds(problem);
        Collection<? extends Route> routes = solver.trySolve();

        assertNotNull("Solver should produce a route collection.", routes);
        Route route = routes.iterator().next();
        assertTrue("Route cost should exceed int range and remain positive.", route.getCostLong() > Integer.MAX_VALUE);
        assertTrue("Printed solution should include the long route cost.", solver.printCurrentSol().contains(String.valueOf(route.getCostLong())));
    }

    @Test
    public void testWRPPRepairSolutionUsesShortestPath() throws Exception {
        WindyGraph original = new WindyGraph(3);
        original.addEdge(1, 2, "required shortcut", 4L, 40L, true);
        original.addEdge(2, 3, "cheap shortcut", 5L, 50L, false);
        original.addEdge(1, 3, "expensive direct", 100L, 100L, false);

        DirectedGraph solution = new DirectedGraph(3);
        solution.addEdge(1, 3, "expensive solution arc", 100L, false);

        WRPPSolver_Win.repairSolution(solution, original);

        assertEquals("The expensive direct arc should be replaced by two shortest-path arcs.", 2, solution.getEdges().size());

        long repairedCost = 0;
        boolean foundFirstSegment = false;
        boolean foundSecondSegment = false;
        for (Arc arc : solution.getEdges()) {
            repairedCost += arc.getCostLong();
            if (arc.getFirstEndpointId() == 1 && arc.getSecondEndpointId() == 2) {
                foundFirstSegment = true;
                assertEquals("First replacement segment should use the forward windy cost.", 4L, arc.getCostLong());
                assertTrue("Required status should come from the original windy edge.", arc.isRequired());
            }
            if (arc.getFirstEndpointId() == 2 && arc.getSecondEndpointId() == 3) {
                foundSecondSegment = true;
                assertEquals("Second replacement segment should use the forward windy cost.", 5L, arc.getCostLong());
            }
        }

        assertEquals("Repaired path should have the cheaper total cost.", 9L, repairedCost);
        assertTrue("Repair should include 1->2.", foundFirstSegment);
        assertTrue("Repair should include 2->3.", foundSecondSegment);
    }

    @Test
    public void testHierholzerAvoidsImmediateBacktrackingWhenAlternativeExists() throws Exception {
        DirectedGraph graph = new DirectedGraph(3);
        graph.setDepotId(1);
        graph.addEdge(1, 2, "a", 1L, true);
        graph.addEdge(2, 1, "b", 1L, true);
        graph.addEdge(2, 3, "c", 1L, true);
        graph.addEdge(3, 2, "d", 1L, true);
        graph.addEdge(1, 3, "e", 1L, true);
        graph.addEdge(3, 1, "f", 1L, true);

        ArrayList<Integer> trail = CommonAlgorithms.tryHierholzer(graph);

        assertEquals("Euler tour should use every edge exactly once.", 6, trail.size());

        TIntObjectHashMap<Arc> indexedEdges = graph.getInternalEdgeMap();
        ArrayList<Integer> visitedVertices = new ArrayList<Integer>();
        int current = graph.getDepotId();
        visitedVertices.add(current);
        for (int edgeId : trail) {
            Arc arc = indexedEdges.get(edgeId);
            assertEquals("Tour should remain contiguous.", current, arc.getTail().getId());
            current = arc.getHead().getId();
            visitedVertices.add(current);
        }

        for (int i = 1; i < visitedVertices.size() - 1; i++) {
            int prev = visitedVertices.get(i - 1);
            int curr = visitedVertices.get(i);
            int next = visitedVertices.get(i + 1);
            assertTrue("Tour should avoid an immediate U-turn at degree-2 vertex 2 when another unused edge exists.",
                    !(curr == 2 && prev == next));
        }
    }

    @Test
    public void testHierholzerAvoidsEnteringForcedReturnBranchTooEarly() throws Exception {
        DirectedGraph graph = new DirectedGraph(4);
        graph.setDepotId(1);
        graph.addEdge(1, 2, "spur-out", 1L, true);
        graph.addEdge(2, 1, "spur-back", 1L, true);
        graph.addEdge(1, 3, "through-out", 1L, true);
        graph.addEdge(3, 4, "through-mid", 1L, true);
        graph.addEdge(4, 1, "through-back", 1L, true);

        Method chooser = CommonAlgorithms.class.getDeclaredMethod("selectHierholzerEdge", oarlib.core.Vertex.class, oarlib.core.Vertex.class, java.util.Map.class);
        chooser.setAccessible(true);

        Arc selected = (Arc) chooser.invoke(null, graph.getVertex(1), null, graph.getVertex(1).getNeighbors());

        assertEquals("Lookahead should prefer the branch that can continue forward instead of entering a forced-return spur.",
                3, selected.getHead().getId());
    }

    @Test
    public void testWinWRPPSolverMode5RegressionFixture() throws Exception {
        Path fixturePath = Paths.get("test_instances", "regressions", "test.oarlib");
        ProblemReader reader = new ProblemReader(ProblemFormat.Name.OARLib);
        WindyGraph graph = (WindyGraph) reader.readGraph(fixturePath.toString());

        WindyCPP problem = new WindyCPP(graph, "mode-5-regression");
        WRPPSolver_Win solver = new WRPPSolver_Win(problem);
        Collection<? extends Route> routes = solver.trySolve();

        assertNotNull("Solver should produce a route collection.", routes);
        assertTrue("Solver should produce at least one route.", !routes.isEmpty());

        Route route = routes.iterator().next();
        assertTrue("Route cost should be positive.", route.getCostLong() > 0);
        assertTrue("Printed solution should include the route cost heading.", solver.printCurrentSol().contains("Route Cost:"));
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
