package mainPack;

import java.util.ArrayList;
import java.util.List;


public class Node {

    private State state;
    private Node father;
    private double finalCost;
    private double distanceCost;
    private double heuristicCost;

    public Node(State state, Node father, double distanceCost ){

        this.state = state;
        this.father = father;
        this.heuristicCost = 0;
        this.distanceCost = distanceCost;
        this.finalCost = heuristicCost + distanceCost;

    }

    /*public Node(State state, Node father, double heuristicCost) {
        this.state = state;
        this.father = father;
        this.heuristicCost = heuristicCost;
        this.distanceCost = state.getDistanceCost();
        this.finalCost = heuristicCost + distanceCost;
    }*/

    public State getState() {
        return state;
    }

    public Node getFather() {
        return father;
    }

    public double getFinalCost() {
        return finalCost;
    }

    public double getDistanceCost() {
        return distanceCost;
    }

    public double getHeuristicCost() {
        return heuristicCost;
    }

    public void updateCosts(double heuristicCost){

        this.heuristicCost = heuristicCost;
        finalCost = distanceCost + heuristicCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return getState() != null ? getState().equals(node.getState()) : node.getState() == null;
    }

    @Override
    public int hashCode() {
        return getState() != null ? getState().hashCode() : 0;
    }
}
