import java.util.*;
import CPP.Absyn.*;

public class RuntimeEnv {
    private Map<String, DFun> sig;
    private LinkedList<Map<String, Value>> ctx;

    public RuntimeEnv() {
        sig = new HashMap<String, DFun>();
        ctx = new LinkedList<Map<String, Value>>();
    }

    public void addFun(String id, DFun f) {
        if (sig.containsKey(id))
            throw new RuntimeException("Function " + id + " already defined.");
        sig.put(id, f);
    }

    public DFun lookupFun(String id) {
        return sig.get(id);
    }

    public Value lookupVar(String id) {
        for (Map<String, Value> c : ctx) {
            Value v = c.get(id);
            if (v != null) return v;
        }
        throw new RuntimeException ("unbound variable " + id);
    }

    public void assignVar(String id, Value v) {
        for (Map<String, Value> c : ctx) {
            Value oldv = c.get(id);
            if (oldv != null) {
                c.put(id, v);
                return;
            }
        }
        throw new RuntimeException ("unbound variable " + id);
    }

    public void newVar(String id, Value v) {
        // Make sure that the variable is not defined in current context.
        if (ctx.peek().containsKey(id))
            throw new RuntimeException ("Variable " + id + " already defined");
        ctx.peek().put(id, v);
    }

    public void newContext() {
        ctx.push(new HashMap<String, Value>());
    }

    public void delContext() {
        ctx.pop();
    }

}
