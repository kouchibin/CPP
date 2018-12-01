import CPP.Absyn.*;
import java.util.*;

public class Interpreter {

    private RuntimeEnv env = new RuntimeEnv();

    private StmVisitor stmVisitor = new StmVisitor();
    private ExpVisitor expVisitor = new ExpVisitor();

    private Scanner s = new Scanner(System.in);

    public void interpret(Program p) {
        p.accept(new ProgramVisitor(), env);
    }

    ////////////////////////////// Programs //////////////////////////////

    public class ProgramVisitor implements Program.Visitor<Void, RuntimeEnv> {
        public Void visit(CPP.Absyn.PDefs p, RuntimeEnv env)
        {
            /* Add all function definitions to the environment. */
            for (Def d: p.listdef_) {
                DFun d1 = (DFun) d;
                env.addFun(d1.id_, d1);
            }

            /* Find main() function. */
            DFun main = env.lookupFun("main");
            if (main == null) throw new RuntimeException("Impossible: main function missing");

            /* Create new scope for main() function. */
            env.newContext();

            /* Execute the main() function body. */
            try {
                for (Stm s: main.liststm_)
                    s.accept(stmVisitor, env);
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

                // If condition is true
                if (condition.value) {
                    env.newContext();
                    p.stm_.accept(stmVisitor, env);
                    env.delContext();
                }
                else
                    return null;
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
                env.newContext();
                p.stm_1.accept(stmVisitor, env);
                env.delContext();
            } else {
                env.newContext();
                p.stm_2.accept(stmVisitor, env);
                env.delContext();
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
                return new VInt(s.nextInt());
            } else if (p.id_.equals("readDouble")) {
                return new VDouble(s.nextDouble());
            } else {
                /* User defined function. */
                DFun fun = env.lookupFun(p.id_);

                /* Bind formal parameters in a new scope. */
                HashMap<String, Value> c = new HashMap<String, Value>();
                int i = 0;
                for (Exp e : p.listexp_) {
                    Value v = e.accept(expVisitor, env);
                    String id = ((ADecl) fun.listarg_.get(i)).id_;
                    c.put(id, v);
                    i++;
                }

                /* Remember where to return after function execution completed. */
                env.setFunctionCallReturnStub();

                /* Push the newly created scope to the stack. */
                env.newContext(c);

                /* Execute function body. */
                try {
                    for (Stm stm : fun.liststm_)
                        stm.accept(stmVisitor, env);
                } catch (ReturnException e) {
                    return e.returnValue;
                } finally {
                    env.resumeCtxAfterFunctionCall();
                }
            }
            return new VVoid();
        }
        public Value visit(CPP.Absyn.EPostIncr p, RuntimeEnv env)
        {
            Value v  = env.lookupVar(p.id_);
            if (v instanceof VInt) {
                VInt v1 = new VInt(((VInt)v).value + 1);
                env.assignVar(p.id_, v1);
            } else if (v instanceof VDouble) {
                VDouble v1 = new VDouble(((VDouble)v).value + 1.0);
                env.assignVar(p.id_, v1);
            }
            return v;
        }
        public Value visit(CPP.Absyn.EPostDecr p, RuntimeEnv env)
        {
            Value v  = env.lookupVar(p.id_);
            if (v instanceof VInt) {
                VInt v1 = new VInt(((VInt)v).value - 1);
                env.assignVar(p.id_, v1);
            } else if (v instanceof VDouble) {
                VDouble v1 = new VDouble(((VDouble)v).value - 1.0);
                env.assignVar(p.id_, v1);
            }
            return v;
        }
        public Value visit(CPP.Absyn.EPreIncr p, RuntimeEnv env)
        {
            Value v  = env.lookupVar(p.id_);
            Value v1 = null;
            if (v instanceof VInt) {
                v1 = new VInt(((VInt)v).value + 1);
                env.assignVar(p.id_, v1);
            } else if (v instanceof VDouble) {
                v1 = new VDouble(((VDouble)v).value + 1.0);
                env.assignVar(p.id_, v1);
            }
            return v1;
        }
        public Value visit(CPP.Absyn.EPreDecr p, RuntimeEnv env)
        {
            Value v  = env.lookupVar(p.id_);
            Value v1 = null;
            if (v instanceof VInt) {
                v1 = new VInt(((VInt)v).value - 1);
                env.assignVar(p.id_, v1);
            } else if (v instanceof VDouble) {
                v1 = new VDouble(((VDouble)v).value - 1.0);
                env.assignVar(p.id_, v1);
            }
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
                VBool result = new VBool(v1.equals(v2));
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool(v1.equals(v2));
                return result;
            } else if (v1 instanceof VBool) {
                VBool result = new VBool(v1.equals(v2));
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
                VBool result = new VBool(!v1.equals(v2));
                return result;
            } else if (v1 instanceof VDouble) {
                VBool result = new VBool(!v1.equals(v2));
                return result;
            } else if (v1 instanceof VBool) {
                VBool result = new VBool(!v1.equals(v2));
                return result;
            } else {
                throw new RuntimeException("Illegal type for comparison.");
            }
        }
        public Value visit(CPP.Absyn.EAnd p, RuntimeEnv env)
        {
            VBool v1 = (VBool) p.exp_1.accept(expVisitor, env);
            if (v1.value == false)
                return new VBool(false);
            VBool v2 = (VBool) p.exp_2.accept(expVisitor, env);
            if (v2.value == true)
                return new VBool(true);
            return new VBool(false);
        }
        public Value visit(CPP.Absyn.EOr p, RuntimeEnv env)
        {
            VBool v1 = (VBool) p.exp_1.accept(expVisitor, env);
            if (v1.value == true)
                return new VBool(true);
            VBool v2 = (VBool) p.exp_2.accept(expVisitor, env);
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
