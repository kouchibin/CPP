import CPP.Absyn.*;
import java.util.*;


public class TypeChecker {

    private Env env = new Env();

    public final Type BOOL   = new Type_bool();
    public final Type INT    = new Type_int();
    public final Type DOUBLE = new Type_double();
    public final Type VOID   = new Type_void();

    private final StmVisitor stmVisitor = new StmVisitor();
    private final ExpVisitor expVisitor = new ExpVisitor();

    public static enum TypeCode {
        CInt, CDouble, CString, CBool, CVoid
    };

    public void typecheck(Program p) {
        p.accept(new ProgramVisitor(), env);
    }

    ////////////////////////////// Program //////////////////////////////

    public class ProgramVisitor implements Program.Visitor<Void, Env> {
        public Void visit(CPP.Absyn.PDefs p, Env env) {

            /* Primitive Functions */
            env.addFun("printInt", new FunType(VOID, singleArg(INT)));
            env.addFun("readInt", new FunType(INT, new ListArg()));
            env.addFun("printDouble", new FunType(VOID, singleArg(DOUBLE)));
            env.addFun("readDouble", new FunType(DOUBLE, new ListArg()));

            /* First pass: construct the function signature lookup table */
            for (Def x : p.listdef_) {
               x.accept(new DefIntoSigVisitor(), env);
            }
            System.out.println(env);

            /* Second pass: check all th */
            for (Def x : p.listdef_) {
                x.accept(new DefVisitor(), env);
            }

            return null;
        }
    }

    public ListArg singleArg(Type t) {
        ListArg l = new ListArg();
        l.add(new ADecl(t, "dummy"));
        return l;
    }

    ////////////////////////////// Function //////////////////////////////

    public class DefIntoSigVisitor implements Def.Visitor<Void, Env> {
        public Void visit(CPP.Absyn.DFun p, Env env) {
            FunType ft = new FunType(p.type_, p.listarg_);
            env.addFun(p.id_, ft);
            return null;
        }
    }

    public class DefVisitor implements Def.Visitor<Void, Env> {
        public Void visit(CPP.Absyn.DFun p, Env env) {
            // set return type and initial context
            env.setReturnType(p.type_);
            env.newContext();

            // add all function parameters to context
            for (Arg a: p.listarg_) {
                a.accept(new ArgVisitor(), env);
            }

            // check function statements
            for (Stm s: p.liststm_) {
                s.accept(stmVisitor, env);
            }

            return null;
        }
    }

    ///////////////////////// Function argument /////////////////////////

    // Add a type declaration to the context
    public class ArgVisitor implements Arg.Visitor<Void, Env> {
        public Void visit(CPP.Absyn.ADecl p, Env env) {
            env.addVar(p.id_, p.type_);
            return null;
        }
    }

    ////////////////////////////// Statement //////////////////////////////

    public class StmVisitor implements Stm.Visitor<Void, Env> {
        public Void visit(CPP.Absyn.SExp p, Env env) {
          Type t = p.exp_.accept(expVisitor, env);
          return null;
        }

        public Void visit(CPP.Absyn.SDecls p, Env env) {
            for (String id : p.listid_)
                env.addVar(id, p.type_);
            return null;
        }

        public Void visit(CPP.Absyn.SInit p, Env env) {
            // p.id_ p.type_
            checkExpr(p.exp_, p.type_);
            env.addVar(p.id_, p.type_);
            return null;
        }

        public Void visit(CPP.Absyn.SReturn p, Env env) {
            checkExpr(p.exp_, env.getReturnType());
            return null;
        }

        public Void visit(CPP.Absyn.SWhile p, Env env) {
            checkExpr(p.exp_, BOOL);
            p.stm_.accept(stmVisitor, env);
            return null;
        }

        public Void visit(CPP.Absyn.SBlock p, Env env) {
            env.newContext();
            for (Stm stm : p.liststm_)
                stm.accept(stmVisitor, env);
            env.delContext();
            return null;
        }

        public Void visit(CPP.Absyn.SIfElse p, Env env) {
            checkExpr(p.exp_, BOOL);
            p.stm_1.accept(stmVisitor, env);
            p.stm_2.accept(stmVisitor, env);
            return null;
        }
  }

    ////////////////////////////// Expression //////////////////////////////

    public class ExpVisitor implements Exp.Visitor<Type, Env> {

        // Literals
        public Type visit(CPP.Absyn.ETrue p, Env env)
        {
          return BOOL;
        }
        public Type visit(CPP.Absyn.EFalse p, Env env)
        {
          return BOOL;
        }
        public Type visit(CPP.Absyn.EInt p, Env env)
        {
          return INT;
        }
        public Type visit(CPP.Absyn.EDouble p, Env env)
        {
          return DOUBLE;
        }

        // Variable
        public Type visit(CPP.Absyn.EId p, Env env)
        {
          return env.lookupVar(p.id_);
        }

        // Function call
        public Type visit(CPP.Absyn.EApp p, Env env)
        {
            FunType ft = env.lookupFun(p.id_);
            if (ft.args.size() != p.listexp_.size())
                throw new TypeException ("wrong number of arguments");
            // check types of arguments
            int i=0;
            for (Exp e : p.listexp_) {
                ADecl a = (ADecl) ft.args.get(i);
                checkExpr(e, a.type_);
                i++;
            }
            return ft.returnType;
        }

        // Increment, decrement

        public Type visit(CPP.Absyn.EPostIncr p, Env env)
        {
            Type t = numericType(env.lookupVar(p.id_));
            return t;
        }

        public Type visit(CPP.Absyn.EPostDecr p, Env env)
        {
            Type t = numericType(env.lookupVar(p.id_));
            return t;
        }
        public Type visit(CPP.Absyn.EPreIncr p, Env env)
        {
            Type t = numericType(env.lookupVar(p.id_));
            return t;
        }
        public Type visit(CPP.Absyn.EPreDecr p, Env env)
        {
            Type t = numericType(env.lookupVar(p.id_));
            return t;
        }

        // Arithmetical operators

        public Type visit(CPP.Absyn.ETimes p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return t1;
        }
        public Type visit(CPP.Absyn.EDiv p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return t1;
        }
        public Type visit(CPP.Absyn.EPlus p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return t1;
        }
        public Type visit(CPP.Absyn.EMinus p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return t1;
        }

        // Comparison operators

        public Type visit(CPP.Absyn.ELt p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return BOOL;
        }
        public Type visit(CPP.Absyn.EGt p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return BOOL;
        }
        public Type visit(CPP.Absyn.ELtEq p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return BOOL;
        }
        public Type visit(CPP.Absyn.EGtEq p, Env env)
        {
            Type t1 = numericType(p.exp_1.accept(expVisitor, env));
            Type t2 = numericType(p.exp_2.accept(expVisitor, env));
            equalTypes(t1, t2);
            return BOOL;
        }

        // Equality operators

        public Type visit(CPP.Absyn.EEq p, Env env)
        {
            Type t1 = p.exp_1.accept(expVisitor, env);
            Type t2 = p.exp_2.accept(expVisitor, env);
            numericType(t1);
            equalTypes(t1, t2);
            return BOOL;
        }
        public Type visit(CPP.Absyn.ENEq p, Env env)
        {
            Type t1 = p.exp_1.accept(expVisitor, env);
            Type t2 = p.exp_2.accept(expVisitor, env);
            numericType(t1);
            equalTypes(t1, t2);
            return BOOL;
        }

        // Logic operators

        public Type visit(CPP.Absyn.EAnd p, Env env)
        {
            boolType(p.exp_1.accept(expVisitor, env));
            boolType(p.exp_2.accept(expVisitor, env));
            return BOOL;
        }
        public Type visit(CPP.Absyn.EOr p, Env env)
        {
            boolType(p.exp_1.accept(expVisitor, env));
            boolType(p.exp_2.accept(expVisitor, env));
            return BOOL;
        }

        // Assignment
        public Type visit(CPP.Absyn.EAss p, Env env)
        {
            Type id_type = env.lookupVar(p.id_);
            Type exp_type = p.exp_.accept(expVisitor, env);
            equalTypes(id_type, exp_type);
            return id_type;
        }
  }


    public void checkExpr (Exp e, Type t) {
        Type t1 = e.accept (new ExpVisitor (), null);
        check(t,t1);
    }
    public void check (Type t, Type u) {
        if (!t.equals(u))
            throw new TypeException("Expected type " + t + ", but found type " + u);
    }
    public Type numericType (Type t) {
        if (!t.equals(INT) && !t.equals(DOUBLE))
            throw new TypeException("expected expression of numeric type");
        return t;
    }
    public Type boolType(Type t) {
        if (!t.equals(BOOL))
            throw new TypeException("expected expression of bool type");
        return t;
    }
    public void equalTypes (Type t1, Type t2) {
        if (!t1.equals(t2))
            throw new TypeException("expected types " + t1 + " and " + t2 + " to be equal");
    }

}
