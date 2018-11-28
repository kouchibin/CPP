import CPP.Absyn.*;
import java.util.*;

public class Interpreter {

    private RuntimeEnv env = new RuntimeEnv();

    private StmVisitor stmVisitor = new StmVisitor();
    private ExpVisitor expVisitor = new ExpVisitor();

    public void interpret(Program p) {
        p.accept(new ProgramVisitor(), env);
    }

    public class ProgramVisitor implements Program.Visitor<Void, RuntimeEnv> {
        public Void visit(CPP.Absyn.PDefs p, RuntimeEnv env)
        {
            /* Add all definitions to the signature */
            for (Def d: p.listdef_) {
                DFun d1 = (DFun) d;
                env.addFun(d1.id_, d1);
            }

            /* Find main function */
            DFun main = env.lookupFun("main");
            if (main == null) throw new RuntimeException("Impossible: main function missing");

            // Initialize context
            env.newContext();

            // Execute the function body
            try {
                for (Stm s: main.liststm_)
                    s.accept(stmVisitor, null);
            } catch (ReturnException e) {}

            return null;
        }
    }

    ////////////////////////////// Statements //////////////////////////////

    public class StmVisitor implements Stm.Visitor<Void, RuntimeEnv>
    {
        public Void visit(CPP.Absyn.SExp p, RuntimeEnv env) {
            Value v = p.exp_.accept(expVisitor, env);
            return null;
        }
        public Void visit(CPP.Absyn.SDecls p, RuntimeEnv env) {
            for (String id: p.listid_) {
                env.newVar(id, new VVoid());
            }
            return null;
        }
        public Void visit(CPP.Absyn.SInit p, RuntimeEnv env)
        {
            Value v = p.exp_.accept(expVisitor, env);
            env.newVar(p.id_, v);
            return null;
        }
        public Void visit(CPP.Absyn.SReturn p, RuntimeEnv env)
        {
            Value v = p.exp_.accept(expVisitor, env);
            throw new ReturnException(v);
        }
        public Void visit(CPP.Absyn.SWhile p, RuntimeEnv env)
        {
            while (true) {
                VBool condition = (VBool) p.exp_.accept(expVisitor, env);
                if (condition.equals(new VBool(false)))
                    return null;
                p.stm_.accept(stmVisitor, env);
            }
        }
        public Void visit(CPP.Absyn.SBlock p, RuntimeEnv env)
        {
            env.newContext();
            for (Stm x: p.liststm_) {
                x.accept(stmVisitor, env);
            }
            env.delContext();
            return null;
        }
        public Void visit(CPP.Absyn.SIfElse p, RuntimeEnv env)
        {
            VBool condition = (VBool) p.exp_.accept(expVisitor, env);
            if (condition.value) {
                p.stm_1.accept(stmVisitor, env);
            } else {
                p.stm_2.accept(stmVisitor, env);
            }
            return null;
        }
    }

    ////////////////////////////// Expressions //////////////////////////////

    public class ExpVisitor implements Exp.Visitor<Value, RuntimeEnv>
    {
        public Value visit(CPP.Absyn.ETrue p, RuntimeEnv env)
        {
            return new VBool(true);
        }
        public Value visit(CPP.Absyn.EFalse p, RuntimeEnv env)
        {
            return new VBool(false);
        }
        public Value visit(CPP.Absyn.EInt p, RuntimeEnv env)
        {
            return new VInt(p.integer_);
        }
        public Value visit(CPP.Absyn.EDouble p, RuntimeEnv env)
        {
            return new VDouble(p.double_);
        }
        public Value visit(CPP.Absyn.EId p, RuntimeEnv env)
        {
            return env.lookupVar(p.id_);
        }
        public Value visit(CPP.Absyn.EApp p, RuntimeEnv env)
        {
            if (p.id_.equals("printInt")) {
                VInt v = (VInt) p.listexp_.get(0).accept(expVisitor, env);
                System.out.println(v.value);
                return new VVoid();
            } else if (p.id_.equals("printDouble")) {
                VDouble v = (VDouble) p.listexp_.get(0).accept(expVisitor, env);
                System.out.println(v.value);
                return new VVoid();
            } else if (p.id_.equals("readInt")) {
                Scanner s = new Scanner(System.in);
                return new VInt(s.nextInt());
            } else if (p.id_.equals("readDouble")) {
                Scanner s = new Scanner(System.in);
                return new VDouble(s.nextDouble());
            } else {
                // Create a new context for function execution
                env.newContext();

                DFun fun = env.lookupFun(p.id_);
                int i = 0;

                // Bind formal parameters
                for (Exp e : p.listexp_) {
                    Value v = e.accept(expVisitor, env);
                    String id = ((ADecl) fun.listarg_.get(i)).id_;
                    env.newVar(id, v);
                    i++;
                }
                try {
                    // Execute function body
                    for (Stm stm : fun.liststm_)
                        stm.accept(stmVisitor, env);
                } catch (ReturnException e) {
                    return e.returnValue;
                } finally {
                    env.delContext();
                }
            }
            return new VVoid();
        }
        public Value visit(CPP.Absyn.EPostIncr p, RuntimeEnv env)
        {
            VInt v  = (VInt) env.lookupVar(p.id_);
            VInt v1 = new VInt(v.value + 1);
            env.assignVar(p.id_, v1);
            return v;
        }
        public Value visit(CPP.Absyn.EPostDecr p, RuntimeEnv env)
        {
            VInt v  = (VInt) env.lookupVar(p.id_);
            VInt v1 = new VInt(v.value - 1);
            env.assignVar(p.id_, v1);
            return v;
        }
        public Value visit(CPP.Absyn.EPreIncr p, RuntimeEnv env)
        {
            VInt v  = (VInt) env.lookupVar(p.id_);
            VInt v1 = new VInt(v.value + 1);
            env.assignVar(p.id_, v1);
            return v1;
        }
        public Value visit(CPP.Absyn.EPreDecr p, RuntimeEnv env)
        {
            VInt v  = (VInt) env.lookupVar(p.id_);
            VInt v1 = new VInt(v.value + 1);
            env.assignVar(p.id_, v1);
            return v1;
        }
        public Value visit(CPP.Absyn.ETimes p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VInt result = new VInt( ((VInt) v1).value * ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VDouble result = new VDouble( ((VDouble) v1).value * ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for multiplication.");
            }
        }
        public Value visit(CPP.Absyn.EDiv p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VInt result = new VInt( ((VInt) v1).value / ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VDouble result = new VDouble( ((VDouble) v1).value / ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for division.");
            }
        }
        public Value visit(CPP.Absyn.EPlus p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VInt result = new VInt( ((VInt) v1).value + ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VDouble result = new VDouble( ((VDouble) v1).value + ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for addition.");
            }
        }
        public Value visit(CPP.Absyn.EMinus p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VInt result = new VInt( ((VInt) v1).value - ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VDouble result = new VDouble( ((VDouble) v1).value - ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for multiplication.");
            }
        }
        public Value visit(CPP.Absyn.ELt p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VBool result = new VBool( ((VInt) v1).value < ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool( ((VDouble) v1).value < ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.EGt p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VBool result = new VBool( ((VInt) v1).value > ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool( ((VDouble) v1).value > ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.ELtEq p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VBool result = new VBool( ((VInt) v1).value <= ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool( ((VDouble) v1).value <= ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.EGtEq p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VBool result = new VBool( ((VInt) v1).value >= ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool( ((VDouble) v1).value >= ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.EEq p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VBool result = new VBool( ((VInt) v1).value == ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool( ((VDouble) v1).value == ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.ENEq p, RuntimeEnv env)
        {
            Value v1 = p.exp_1.accept(expVisitor, env);
            Value v2 = p.exp_2.accept(expVisitor, env);
            if (v1 instanceof VInt) {
                VBool result = new VBool( ((VInt) v1).value != ((VInt) v2).value);
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool( ((VDouble) v1).value != ((VDouble) v2).value);
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.EAnd p, RuntimeEnv env)
        {
            VBool v1 = (VBool) p.exp_1.accept(expVisitor, env);
            VBool v2 = (VBool) p.exp_2.accept(expVisitor, env);
            if (v1.value == false)
                return new VBool(false);
            else if (v2.value == true)
                return new VBool(true);
            else
                return new VBool(false);
        }
        public Value visit(CPP.Absyn.EOr p, RuntimeEnv env)
        {
            VBool v1 = (VBool) p.exp_1.accept(expVisitor, env);
            VBool v2 = (VBool) p.exp_2.accept(expVisitor, env);
            if (v1.value == true)
                return new VBool(true);
            if (v2.value == true)
                return new VBool(true);
            return new VBool(false);
        }
        public Value visit(CPP.Absyn.EAss p, RuntimeEnv env)
        {
            Value v = p.exp_.accept(expVisitor, env);
            env.assignVar(p.id_, v);
            return v;
        }
    }
}
