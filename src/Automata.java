import java.util.*;

public class Automata {
    private HashMap<String, State> stateMap;
    private List<String> alphabet;
    private String type;
    private String mod;

    public Automata(List<String> alph, String type, String mod)
    {
        alphabet = alph;
        stateMap = new HashMap<String, State>();
        this.type = type.toLowerCase();
        this.mod = mod.toLowerCase();
    }

    public void addState(String name)
    {
        State newState = new State(name);
        stateMap.put(name, newState);
    }

    public State getState(String name)
    {
        return stateMap.get(name);
    }

    public Boolean isNFA(){
        for (State state : stateMap.values()) {
            HashMap<String, Boolean> seen = new HashMap<String, Boolean>();
            for (List<String> transitions : state.getTransitionsOut().values()) {
                for (String transition : transitions) {
                    if (seen.containsKey(transition)) {
                        return true;
                    }
                    seen.put(transition, true);
                }
            }
        }
        return false;
    }

    public Boolean isDFA(){
        for (State state : stateMap.values()) {
            HashMap<String, Boolean> seen = new HashMap<String, Boolean>();
            for (List<String> transitions : state.getTransitionsOut().values()) {
                for (String transition : transitions) {
                    if (transition.equals("epsilon")) {
                        return false; 
                    }
                    if (seen.containsKey(transition)) {
                        return false;
                    }
                    seen.put(transition, true);
                }
            }
        }
        return true;
    }

    public Boolean isCompleteDFA(){
        for (State state : stateMap.values()) {
            HashMap<String, Boolean> seen = new HashMap<String, Boolean>();
            for (List<String> transitions : state.getTransitionsOut().values()) {
                for (String transition : transitions) {
                    if (transition.equals("epsilon")) {
                        return false; 
                    }
                    if (seen.containsKey(transition)) {
                        return false;
                    }
                    seen.put(transition, true);
                }
            }
            for (String letter : alphabet) {
                if (!seen.containsKey(letter)) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<State> getStates(){
        List<State> states = new ArrayList<State>();
        for (String key : stateMap.keySet()) {
            states.add(stateMap.get(key));
        }
        return states;
    }

    public void verify(){
        if (type.equals("dfa")) {
            if (mod.equals("complete")){
                if (!isCompleteDFA()) {
                    System.out.println("Error: DFA is not complete.");
                    System.exit(1);
                }
            } else {
                if (!isDFA()) {
                    System.out.println("Error: DFA is not deterministic.");
                    System.exit(1);
                }
            }
        } else {
            if (!isNFA()) {
                System.out.println("Error: NFA is not nondeterministic.");
                System.exit(1);
            }
        }
    }
}
