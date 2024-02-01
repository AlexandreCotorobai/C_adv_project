import java.util.*;

public class State {
    private String name;
    private String coords;
    private HashMap<String, ArrayList<String>> transitions_in;
    private HashMap<String, ArrayList<String>> transitions_out;
    private boolean accepting = false;
    private boolean initial = false;

    public State(String name) {
        this.name = name;
        this.transitions_in = new HashMap<String, ArrayList<String>>();
        this.transitions_out = new HashMap<String, ArrayList<String>>();
    }

    // Getters
    public String getName() {
        return this.name;
    }

    public String getCoords() {
        return this.coords;
    }

    public HashMap<String, ArrayList<String>> getTransitionsIn() {
        return this.transitions_in; 
    }

    public HashMap<String, ArrayList<String>> getTransitionsOut() {
        return this.transitions_out;
    }

    public boolean isAccepting() {
        return this.accepting;
    }

    public boolean isInitial() {
        return this.initial;
    }

    // Setters

    public void setCoords(String coords) {
        this.coords = coords;
    }

    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    // Adders

    public void addTransitionIn(String transition, String state) {
        if (this.transitions_in.containsKey(state)) {
            this.transitions_in.get(state).add(transition);
        } else {
            ArrayList<String> transitions = new ArrayList<String>();
            transitions.add(transition);
            this.transitions_in.put(state, transitions);
        }
    }

    public void addTransitionOut(String transition, String state) {
        if (this.transitions_out.containsKey(state)) {
            this.transitions_out.get(state).add(transition);
        } else {
            ArrayList<String> transitions = new ArrayList<String>();
            transitions.add(transition);
            this.transitions_out.put(state, transitions);
        }
    }

    // Other

    public String toString() {
        String str = "";
        str += "State: " + this.name + "\n";
        str += "Coords: " + this.coords + "\n";
        str += "Transitions In: " + this.transitions_in + "\n";
        str += "Transitions Out: " + this.transitions_out + "\n";
        str += "Accepting: " + this.accepting + "\n";
        str += "Initial: " + this.initial + "\n";
        return str;
    }
    
}
