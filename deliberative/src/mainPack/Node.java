package mainPack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lorenzotara on 21/10/17.
 */
public class Node {

    private State state;
    private State father;
    private List<State> children;

    public Node(State state, State father) {
        this.state = state;
        this.father = father;
        this.children = new ArrayList<>();
    }

    public Node addChild(State child) {

        children.add(child);

        return new Node(child, this.state);

    }

    public State getState() {
        return state;
    }

    public State getFather() {
        return father;
    }

    public List<State> getChildren() {
        return children;
    }
}
