package network;

import agent.Agent;
import java.util.*;
import rand.randomGenerator;

public class ConnectingNearestNeighborNetwork extends Network {

    private double p;
    
    // O(1) Random Access and O(1) Contains Check for Potential Edges
    private List<Edge> potentialEdgeList;
    private Set<Edge> potentialEdgeSet;
    
    // Degree Cache for O(1) lookups
    private int[] nodeDegrees;

    private static class Edge {
        int from, to;

        Edge(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge)) return false;
            Edge other = (Edge) o;
            return this.from == other.from && this.to == other.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

    public ConnectingNearestNeighborNetwork(int size, double p) {
        super(size);
        this.p = p;
        this.potentialEdgeList = new ArrayList<>();
        this.potentialEdgeSet = new HashSet<>();
        this.nodeDegrees = new int[size];
    }

    /**
     * Wrapper for setEdge that safely updates the degree cache in O(1) time.
     * It ensures we are counting unique neighbors (undirected degree equivalent),
     * matching the logic of the original O(N) loop.
     */
    private void addEdgeWithCache(int from, int to, double weight) {
        boolean alreadyConnected = (adjacencyMatrix[from][to] > 0 || adjacencyMatrix[to][from] > 0);
        
        setEdge(from, to, weight);
        
        if (!alreadyConnected) {
            nodeDegrees[from]++;
            nodeDegrees[to]++;
        }
    }

    private void addPotentialEdge(int from, int to) {
        Edge e = new Edge(from, to);
        // Only add to the list if it wasn't already in the set
        if (potentialEdgeSet.add(e)) {
            potentialEdgeList.add(e);
        }
    }

    @Override
    public void makeNetwork(Agent[] agentSet) {
        System.out.println("start making network");

        double r = 0.01;
        double reciprocityProb = 0.2;

        int currentSize = 2;

        addEdgeWithCache(0, 1, 1);
        addEdgeWithCache(1, 0, 1);

        while (currentSize < getSize()) {
            if (randomGenerator.get().nextDouble() < 1 - this.p) {
                // add a new node 
                int newNode = currentSize++;
                // a new node is connected to an existing node based on similarity
                int v = chooseNodeBySimilarity(newNode, agentSet);
                addEdgeWithCache(newNode, v, 1);
                
                if (randomGenerator.get().nextDouble() < reciprocityProb) {
                    addEdgeWithCache(v, newNode, 1);
                }

                for (int neighbor = 0; neighbor < newNode; neighbor++) {
                    if (adjacencyMatrix[v][neighbor] > 0 && neighbor != newNode) {
                        addPotentialEdge(newNode, neighbor);
                        if (randomGenerator.get().nextDouble() < reciprocityProb) {
                            addPotentialEdge(neighbor, newNode);
                        }
                    }
                }
            } else {
                if (randomGenerator.get().nextDouble() < 1 - r) { // CNN with random links (CNNR)
                    // convert potential edge to actual edge in O(1) time
                    if (!potentialEdgeList.isEmpty()) {
                        int randIndex = randomGenerator.get().nextInt(potentialEdgeList.size());
                        Edge edge = potentialEdgeList.get(randIndex);
                        
                        // O(1) removal by swapping with the last element
                        Edge lastEdge = potentialEdgeList.get(potentialEdgeList.size() - 1);
                        potentialEdgeList.set(randIndex, lastEdge);
                        potentialEdgeList.remove(potentialEdgeList.size() - 1);
                        potentialEdgeSet.remove(edge);

                        addEdgeWithCache(edge.from, edge.to, 1);
                        if (randomGenerator.get().nextDouble() < reciprocityProb) {
                            addEdgeWithCache(edge.to, edge.from, 1);
                        }
                    }
                } else {
                    // add link randomly with FAIL-SAFE limit
                    int a;
                    int attemptsA = 0;
                    int maxAttempts = 50; 
                    
                    // Fail-safe: Prevent infinite loop if 'a' is already fully connected
                    do {
                        a = randomGenerator.get().nextInt(currentSize);
                        attemptsA++;
                    } while (nodeDegrees[a] >= currentSize - 1 && attemptsA < maxAttempts);

                    // Only proceed if we found a valid, non-saturated node
                    if (nodeDegrees[a] < currentSize - 1) {
                        int b;
                        int attemptsB = 0;
                        do {
                            b = randomGenerator.get().nextInt(currentSize);
                            attemptsB++;
                        } while ((a == b || adjacencyMatrix[a][b] > 0) && attemptsB < maxAttempts);

                        if (a != b && adjacencyMatrix[a][b] == 0) {
                            addEdgeWithCache(a, b, 1);
                            if (randomGenerator.get().nextDouble() < reciprocityProb) {
                                addEdgeWithCache(b, a, 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private int chooseNodeBySimilarity(int maxIndex, Agent[] agents) {
        double alpha = 0.7;   // degree effect
        double lambda = 5.0;  // similarity decay

        double[] weights = new double[maxIndex];
        double sum = 0.0;

        int i = maxIndex; // new node index

        for (int j = 0; j < maxIndex; j++) {
            // O(1) lookup replaces the O(N) internal loop
            int degree = nodeDegrees[j];
            double degreeTerm = Math.pow(degree + 1, alpha);

            // similarity
            double diff = Math.abs(agents[i].getIntrinsicOpinion() - agents[j].getIntrinsicOpinion());
            double sim = Math.exp(-lambda * diff);

            weights[j] = degreeTerm * sim;
            sum += weights[j];
        }

        // Failsafe in case sum is 0 (prevents division by zero or infinite loops)
        if (sum == 0) return randomGenerator.get().nextInt(maxIndex); 

        double r = randomGenerator.get().nextDouble() * sum;
        double cum = 0.0;

        for (int j = 0; j < maxIndex; j++) {
            cum += weights[j];
            if (cum >= r) {
                return j;
            }
        }

        return maxIndex - 1; // fallback
    }
}