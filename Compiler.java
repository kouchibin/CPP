import java.util.*;
import CPP.Absyn.*;

public class Compiler
{
  // The output of the compiler is a list of strings.
  LinkedList<String> output;

  // Signature mapping function names to their JVM name and type.
  Map<String,Fun> sig;

  // Context mapping variable identifiers to their type and address.
  List<Map<String,CxtEntry>> cxt;

  // Next free address for local variable;
  int nextLocal = 0;

  // Number of locals needed for current function
  int limitLocals = 0;

  // Maximum stack size needed for current function
  int limitStack = 0;

  // Current stack size
  int currentStack = 0;

  // Global counter to get next label;
  int nextLabel = 0;


  // Variable information
  public class CxtEntry {
    final Type    type;
    final Integer addr;
    CxtEntry (Type t, Integer a) { type = t; addr = a; }
  }

  // Share type constants
  public final Type BOOL   = new Type_bool  ();
  public final Type INT    = new Type_int   ();
  public final Type DOUBLE = new Type_double();
  public final Type VOID   = new Type_void  ();

  // Compile C-- AST to a .j source file (returned as String).
  // name should be just the class name without file extension.
  public String compile(String name, CPP.Absyn.Program p) {
    // Initialize output
    output = new LinkedList();

    // Output boilerplate
    output.add(".class public " + name + "\n");
    output.add(".super java/lang/Object\n");
    output.add("\n");
    output.add(".method public <init>()V\n");
    output.add("  .limit locals 1\n");
    output.add("\n");
    output.add("  aload_0\n");
    output.add("  invokespecial java/lang/Object/<init>()V\n");
    output.add("  return\n");
    output.add("\n");
    output.add(".end method\n");
    output.add("\n");
    output.add(".method public static main([Ljava/lang/String;)V\n");
    output.add("  .limit locals 1\n");
    output.add("  .limit stack  1\n");
    output.add("\n");
    output.add("  invokestatic " + name + "/main()I\n");
    output.add("  pop\n");
    output.add("  return\n");
    output.add("\n");
    output.add(".end method\n");
    output.add("\n");

    // Create signature
    sig = new TreeMap();

    // Built-in functions
    ListArg intArg = new ListArg();
    intArg.add (new ADecl(INT , "x"));
    ListArg doubleArg = new ListArg();
    doubleArg.add (new ADecl(DOUBLE , "x"));
    ListArg voidArg = new ListArg();
    sig.put("printInt",    new Fun ("Runtime/printInt"   , new FunType (VOID  , intArg)));
    sig.put("readInt",    new Fun ("Runtime/readInt"   , new FunType (INT  , voidArg)));
    sig.put("printDouble",    new Fun ("Runtime/printDouble"   , new FunType (VOID  , doubleArg)));
    sig.put("readDouble",    new Fun ("Runtime/readDouble"   , new FunType (VOID  , voidArg)));



    // User-defined functions
    for (Def d: ((PDefs)p).listdef_) {
      DFun def = (DFun)d;
      sig.put(def.id_,
        new Fun(name + "/" + def.id_, new FunType(def.type_, def.listarg_)));
    }

    // Run compiler
    p.accept(new ProgramVisitor(), null);

    // Concatenate strings in output to .j file content.
    StringBuilder jtext = new StringBuilder();
    for (String s: output) {
      jtext.append(s);
    }
    return jtext.toString();
  }

  // Compile program

  public class ProgramVisitor implements Program.Visitor<Void,Void>
  {
    public Void visit(CPP.Absyn.PDefs p, Void arg)
    {
      for (Def def: p.listdef_)
        def.accept(new DefVisitor(), null);
      return null;
    }
  }

  // Compile function definition.

  public class DefVisitor implements Def.Visitor<Void,Void>
  {
    public Void visit(CPP.Absyn.DFun p, Void arg)
    {
      // reset state for new function
      cxt = new LinkedList();
      cxt.add(new TreeMap());
      nextLocal    = 0;
      limitLocals  = 0;
      limitStack   = 0;
      currentStack = 0;

      // save output so far and reset output;
      LinkedList<String> savedOutput = output;
      output = new LinkedList();

      // Compile function

      // Add function parameters to context
      for (Arg x: p.listarg_)
        x.accept (new ArgVisitor(), null);

      Stm last_stm = null;
      for (Stm s: p.liststm_) {
        s.accept (new StmVisitor(), null);
        last_stm = s;
      }


      // If the last statement of a function is not SReturn,
      // then add a return statement.
      if (!(last_stm instanceof SReturn))
        emit(new Return(VOID));

      // add new Output to old output
      LinkedList<String> newOutput = output;
      output = savedOutput;

      Fun f = new Fun(p.id_, new FunType(p.type_, p.listarg_));
      output.add("\n.method public static " + f.toJVM() + "\n");
      output.add("  .limit locals " + limitLocals + "\n");
      output.add("  .limit stack " + limitStack + "\n\n");
      for (String s: newOutput) {
        output.add("  " + s);
      }
      output.add("\n.end method\n");
      return null;
    }
  }

  // "Compiling" a function argument means adding it to the context.

  public class ArgVisitor implements Arg.Visitor<Void,Void>
  {
    public Void visit(CPP.Absyn.ADecl p, Void arg)
    {
      newVar (p.id_, p.type_);
      return null;
    }
  }

  // Compile single statement.

  public class StmVisitor implements Stm.Visitor<Void,Void>
  {
    // e;
    public Void visit(CPP.Absyn.SExp p, Void arg)
    {
      emit (new Comment(CPP.PrettyPrinter.print(p)));
      p.exp_.accept (new ExpVisitor(), arg);
      if (!p.exp_.getType().equals(VOID)) {
        emit (new Pop(p.exp_.getType()));
      }
      return null;
    }

    // int x,y,z;
    public Void visit(CPP.Absyn.SDecls p, Void arg)
    {
      emit (new Comment(CPP.PrettyPrinter.print(p)));
      for (String x: p.listid_) {
        newVar(x, p.type_);
      }
      return null;
    }

    // int x = e;
    public Void visit(CPP.Absyn.SInit p, Void arg)
    {
      // p.type_ p.id_ p.exp_
      emit (new Comment(CPP.PrettyPrinter.print(p)));
      p.exp_.accept (new ExpVisitor(), arg);
      int addr = newVar (p.id_, p.type_);
      emit (new Store (p.type_, addr));
      return null;
    }

    // return e;
    public Void visit(CPP.Absyn.SReturn p, Void arg)
    {
      // p.exp_getType()
      emit (new Comment(CPP.PrettyPrinter.print(p)));
      p.exp_.accept (new ExpVisitor(), arg);
      emit (new Return(p.exp_.getType()));
      return null;
    }

    // while (e) s
    public Void visit(CPP.Absyn.SWhile p, Void arg)
    {
      // p.exp_ p.stm_
      emit (new Comment("test while-condition (" + CPP.PrettyPrinter.print(p.exp_) + ")\n"));
      Label start = new Label (nextLabel++);
      Label done  = new Label (nextLabel++);
      emit (new Target(start));
      p.exp_.accept (new ExpVisitor(), arg);
      emit (new IfZ(done));
      emit (new Comment("while (" + CPP.PrettyPrinter.print(p.exp_) + ") do:\n"));
      newBlock();
      p.stm_.accept (this, arg);
      popBlock();
      emit (new Goto(start));
      emit (new Target(done));
      return null;
    }

    // { ss }
    public Void visit(CPP.Absyn.SBlock p, Void arg)
    {
      newBlock();
      for (Stm s: p.liststm_) s.accept(this, arg);
      popBlock();
      return null;
    }

    // if (e) s else s'
    public Void visit(CPP.Absyn.SIfElse p, Void arg)
    {
      Label t = new Label (nextLabel++);
      Label f = new Label (nextLabel++);
      emit (new Comment("test if-condition (" + CPP.PrettyPrinter.print(p.exp_) + ")\n"));
      p.exp_.accept(new ExpVisitor(), arg);
      emit (new IfZ(f));

      emit (new Comment("when (" + CPP.PrettyPrinter.print(p.exp_) + ") do: \n"));
      newBlock();
      p.stm_1.accept(this, arg);
      popBlock();
      emit (new Goto(t));

      emit (new Comment("unless (" + CPP.PrettyPrinter.print(p.exp_) + ") do: \n"));
      emit (new Target(f));
      newBlock();
      p.stm_2.accept(this, arg);
      popBlock();
      emit(new Target(t));

      // Place holder for preventing dangling label
      emit(new Nop());
      return null;
    }
  }

  public class ExpVisitor implements Exp.Visitor<Void,Void>
  {
    // true
    public Void visit(CPP.Absyn.ETrue p, Void arg)
    {
      emit(new IConst(1));
      return null;
    }

    // false
    public Void visit(CPP.Absyn.EFalse p, Void arg)
    {
      emit(new IConst(0));
      return null;
    }

    // 5
    public Void visit(CPP.Absyn.EInt p, Void arg)
    {
      emit (new IConst (p.integer_));
      return null;
    }

    // 3.14
    public Void visit(CPP.Absyn.EDouble p, Void arg)
    {
      throw new RuntimeException ("TODO: compile " + CPP.PrettyPrinter.print(p));
    }

    // x
    public Void visit(CPP.Absyn.EId p, Void arg)
    {
      CxtEntry ce = lookupVar(p.id_);
      emit (new Load (ce.type, ce.addr));
      return null;
    }

    // f (e_1, ..., e_n)
    public Void visit(CPP.Absyn.EApp p, Void arg)
    {
      // p.id_
      for (Exp e: p.listexp_) e.accept (new ExpVisitor(), arg);
      Fun f = sig.get(p.id_);
      emit (new Call(f));
      return null;
    }

    // x++
    public Void visit(CPP.Absyn.EPostIncr p, Void arg)
    {
      // p.id_
      CxtEntry ce = lookupVar(p.id_);
      emit(new Load(ce.type, ce.addr));
      emit(new Dup());
      emit(new IConst(1));
      emit(new Add(INT));
      emit(new Store(ce.type, ce.addr));
      return null;
    }

    // x--
    public Void visit(CPP.Absyn.EPostDecr p, Void arg)
    {
      // p.id_
      CxtEntry ce = lookupVar(p.id_);
      emit(new Load(ce.type, ce.addr));
      emit(new Dup());
      emit(new IConst(-1));
      emit(new Add(INT));
      emit(new Store(ce.type, ce.addr));
      return null;
    }

    // ++x
    public Void visit(CPP.Absyn.EPreIncr p, Void arg)
    {
      // p.id_
      CxtEntry ce = lookupVar(p.id_);
      emit(new Load(ce.type, ce.addr));
      emit(new IConst(1));
      emit(new Add(INT));
      emit(new Dup());
      emit(new Store(ce.type, ce.addr));
      return null;
    }

    // --x
    public Void visit(CPP.Absyn.EPreDecr p, Void arg)
    {
      // p.id_
      CxtEntry ce = lookupVar(p.id_);
      emit(new Load(ce.type, ce.addr));
      emit(new IConst(-1));
      emit(new Add(INT));
      emit(new Dup());
      emit(new Store(ce.type, ce.addr));
      return null;
    }

    // e * e'
    public Void visit(CPP.Absyn.ETimes p, Void arg)
    {
      p.exp_1.accept (this, arg);
      p.exp_2.accept (this, arg);
      emit(new Mul(p.getType()));
      return null;
    }

    // e / e'
    public Void visit(CPP.Absyn.EDiv p, Void arg)
    {
      p.exp_1.accept (this, arg);
      p.exp_2.accept (this, arg);
      emit(new Div(p.getType()));
      return null;
    }

    //  e + e'
    public Void visit(CPP.Absyn.EPlus p, Void arg)
    {
      // p.exp_1 p.exp_2 p.getType()
      p.exp_1.accept (this, arg);
      p.exp_2.accept (this, arg);
      emit (new Add(p.getType()));
      return null;
    }

    // e - e'
    public Void visit(CPP.Absyn.EMinus p, Void arg)
    {
      p.exp_1.accept (this, arg);
      p.exp_2.accept (this, arg);
      emit (new Minus(p.getType()));
      return null;
    }

    // e < e'
    public Void visit(CPP.Absyn.ELt p, Void arg)
    {
      // p.exp_1 p.exp_2
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Label yes  = new Label (nextLabel++);
      Label done = new Label (nextLabel++);
      emit (new IfLt(p.getType(), yes));
      emit (new IConst(0));
      emit (new Goto(done));
      emit (new Target(yes));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // e > e'
    public Void visit(CPP.Absyn.EGt p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Label yes  = new Label (nextLabel++);
      Label done = new Label (nextLabel++);
      emit (new IfGt(p.getType(), yes));
      emit (new IConst(0));
      emit (new Goto(done));
      emit (new Target(yes));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // e <= e'
    public Void visit(CPP.Absyn.ELtEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Label yes  = new Label (nextLabel++);
      Label done = new Label (nextLabel++);
      emit (new IfLe(p.getType(), yes));
      emit (new IConst(0));
      emit (new Goto(done));
      emit (new Target(yes));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // e >= e'
    public Void visit(CPP.Absyn.EGtEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Label yes  = new Label (nextLabel++);
      Label done = new Label (nextLabel++);
      emit (new IfGe(p.getType(), yes));
      emit (new IConst(0));
      emit (new Goto(done));
      emit (new Target(yes));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // e == e'
    public Void visit(CPP.Absyn.EEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Label yes  = new Label (nextLabel++);
      Label done = new Label (nextLabel++);
      emit (new IfEq(p.getType(), yes));
      emit (new IConst(0));
      emit (new Goto(done));
      emit (new Target(yes));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // e != e'
    public Void visit(CPP.Absyn.ENEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Label yes  = new Label (nextLabel++);
      Label done = new Label (nextLabel++);
      emit (new IfNEq(p.getType(), yes));
      emit (new IConst(0));
      emit (new Goto(done));
      emit (new Target(yes));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // e && e'
    public Void visit(CPP.Absyn.EAnd p, Void arg)
    {
      Label done = new Label (nextLabel++);
      p.exp_1.accept(new ExpVisitor(), arg);
      emit (new Dup());
      emit (new IfZ(done));
      p.exp_2.accept(new ExpVisitor(), arg);
      emit (new IfNZ(done));
      emit (new Pop(p.getType()));
      emit (new IConst(0));
      emit (new Target(done));
      return null;
    }

    // e || e'
    public Void visit(CPP.Absyn.EOr p, Void arg)
    {
      Label done = new Label (nextLabel++);
      p.exp_1.accept(new ExpVisitor(), arg);
      emit (new Dup());
      emit (new IfNZ(done));
      p.exp_2.accept(new ExpVisitor(), arg);
      emit (new IfZ(done));
      emit (new Pop(p.getType()));
      emit (new IConst(1));
      emit (new Target(done));
      return null;
    }

    // x = e
    public Void visit(CPP.Absyn.EAss p, Void arg)
    {
      // p.id_ ce.type ce.addr
      p.exp_.accept (this, arg);
      CxtEntry ce = lookupVar (p.id_);
      emit (new Store (ce.type, ce.addr));
      emit (new Load (ce.type, ce.addr));
      return null;
    }
  }

  void emit(Code code) {
    if (code instanceof Store || code instanceof Pop  ||
        code instanceof IfZ   || code instanceof IfNZ ||
        code instanceof Add   || code instanceof Mul  ||
        code instanceof Div   || code instanceof Minus) {
      currentStack--;
    }

    else if (code instanceof Load || code instanceof IConst ||
             code instanceof Dup) {
      incStack();
    }

    else if (code instanceof IfLt || code instanceof IfGt ||
             code instanceof IfLe || code instanceof IfGe ||
             code instanceof IfEq || code instanceof IfNEq) {
      currentStack -= 2;
    }

    else if (code instanceof Call) {

      // Calling function will decrease the stack by the number of parameters
      int arg_count = ((Call)code).fun.funType.args.size();
      currentStack -= arg_count;

      // If the function has return value, stack size will increase by 1
      if (!((Call)code).fun.funType.returnType.equals(VOID))
        incStack();

    }
    String jvm = code.accept(new CodeToJVM());
    output.add(jvm);
  }

  int newVar(String id, Type t) {
    cxt.get(0).put(id, new CxtEntry(t, limitLocals++));
    return limitLocals - 1;
  }

  CxtEntry lookupVar(String id) {
    for (Map<String, CxtEntry> m : cxt) {
      if (m.containsKey(id))
        return m.get(id);
    }
    throw new RuntimeException("Variable " + id + " not found.");
  }

  void incStack() {
    currentStack++;
    if (currentStack > limitStack)
      limitStack = currentStack;
  }

  void newBlock() {
    cxt.add(0, new TreeMap());
  }

  void popBlock() {
    cxt.remove(0);
  }

}
