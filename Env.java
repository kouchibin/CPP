import java.util.*;
import CPP.Absyn.*;

public class Env {
    public static HashMap<String, FunType> signature;           // Funtion signatures
    public static LinkedList<HashMap<String, Type>> contexts;   // Scopes
    private static Type returnType;                             // Current function return types

    public Env() {
        signature = new HashMap<String, FunType>();
        contexts = new LinkedList<HashMap<String, Type>>();
        returnType = null;
    }


    public Type lookupVar(String id) {
        for (Map<String, Type> ctx : contexts) {
            Type t = ctx.get(id);
            if (t != null) return t;
        }
        throw new TypeException("Unbound variable " + id);
    }

    public FunType lookupFun(String id) {
        if (!signature.containsKey(id))
            throw new TypeException("Function " + id + "not defined.");
        return signature.get(id);
    }

    public void addVar(String id, Type ty) {
        if (contexts.peek().containsKey(id))
                throw new TypeException("Variable " + id + " already defined.");
        contexts.peek().put(id, ty);
    }

    public void addFun(String id, FunType ty) {
        if (signature.get(id) != null)
            throw new TypeException("Function " + id + " already exists.");
        signature.put(id, ty);
    }

    public void setReturnType(Type t) {
        returnType = t;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void newContext() {
        contexts.push(new HashMap<String, Type>());
    }

    public void delContext() {
        contexts.pop();
    }

    public String toString() {
        return "Signature: " + signature.toString() +
               "\nContext: " + contexts.toString();
    }
}
