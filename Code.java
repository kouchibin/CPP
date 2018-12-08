// Code.java
// Originally created by github.com/andreasabel/java-adt

import CPP.Absyn.*;

class Fun {
    public String id;
    public FunType funType;
    public Fun (String id, FunType funType) {
        this.id = id;
        this.funType = funType;
    }
    public String toJVM() {
      return id + funType.toJVM();
    }
}

class Label {
    public int label;
    public Label (int label) {
        this.label = label;
    }
    public String toJVM() {
      return "L" + label;
    }
}

abstract class Code {
    public abstract <R> R accept (CodeVisitor<R> v);
}

class Comment extends Code {
  public String comment;
  public Comment (String c) { comment = c; }
  public <R> R accept (CodeVisitor<R> v) {
    return v.visit(this);
  }
}

class Store extends Code {
    public Type type;
    public Integer addr;
    public Store (Type type, Integer addr) {
        this.type = type;
        this.addr = addr;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Load extends Code {
    public Type type;
    public Integer addr;
    public Load (Type type, Integer addr) {
        this.type = type;
        this.addr = addr;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IConst extends Code {
    public Integer immed;
    public IConst (Integer immed) {
        this.immed = immed;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Pop extends Code {
    public Type type;
    public Pop (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Return extends Code {
    public Type type;
    public Return (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Call extends Code {
    public Fun fun;
    public Call (Fun fun) {
        this.fun = fun;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Target extends Code {
    public Label label;
    public Target (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Goto extends Code {
    public Label label;
    public Goto (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfZ extends Code {
    public Label label;
    public IfZ (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfNZ extends Code {
    public Label label;
    public IfNZ (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfLt extends Code {
    public Type type;
    public Label label;
    public IfLt (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfGt extends Code {
    public Type type;
    public Label label;
    public IfGt (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfLe extends Code {
    public Type type;
    public Label label;
    public IfLe (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfGe extends Code {
    public Type type;
    public Label label;
    public IfGe (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfEq extends Code {
    public Type type;
    public Label label;
    public IfEq (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfNEq extends Code {
    public Type type;
    public Label label;
    public IfNEq (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}



class Add extends Code {
    public Type type;
    public Add (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Mul extends Code {
    public Type type;
    public Mul (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Div extends Code {
    public Type type;
    public Div (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Minus extends Code {
    public Type type;
    public Minus (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Dup extends Code {
    public <R> R accept(CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Nop extends Code {
    public <R> R accept(CodeVisitor<R> v) {
        return v.visit (this);
    }
}

interface CodeVisitor<R> {
    public R visit (Comment c);
    public R visit (Store c);
    public R visit (Load c);
    public R visit (IConst c);
    public R visit (Pop c);
    public R visit (Return c);
    public R visit (Call c);
    public R visit (Target c);
    public R visit (Goto c);
    public R visit (IfZ c);
    public R visit (IfNZ c);
    public R visit (IfLt c);
    public R visit (IfGt c);
    public R visit (IfLe c);
    public R visit (IfGe c);
    public R visit (IfEq c);
    public R visit (IfNEq c);
    public R visit (Add c);
    public R visit (Mul c);
    public R visit (Div c);
    public R visit (Minus c);
    public R visit (Dup c);
    public R visit (Nop c);
}

class CodeToJVM implements CodeVisitor<String> {

    public String visit (Comment c) {
        return "\n  ;; " + c.comment;
    }
    public String visit (Store c) {
        if (c.addr <= 3)
            return "istore_" + c.addr + "\n";
        return "istore " + c.addr + "\n";
    }
    public String visit (Load c) {
        if (c.addr <= 3)
            return "iload_" + c.addr + "\n";
        return "iload " + c.addr + "\n";
    }
    public String visit (IConst c) {
        if (c.immed >= 0 && c.immed <= 5)
            return "iconst_" + c.immed + "\n";
        if (c.immed == -1)
            return "iconst_m1\n";
        return "ldc " + c.immed + "\n";
    }
    public String visit (Pop c) {
        return "pop \n";
    }
    public String visit (Return c) {
        String result = "";
        if (c.type instanceof Type_int || c.type instanceof Type_bool)
            result = "ireturn \n";
        if (c.type instanceof Type_void)
            result = "return \n";
        return result;
    }
    public String visit (Call c) {
        return "invokestatic " + c.fun.toJVM() + "\n";
    }
    public String visit (Target c) {
        return c.label.toJVM() + ":\n";
    }
    public String visit (Goto c) {
        return "goto " + c.label.toJVM() + "\n";
    }
    public String visit (IfZ c) {
        return "ifeq " + c.label.toJVM() + "\n";
    }
    public String visit (IfNZ c) {
        return "ifne " + c.label.toJVM() + "\n";
    }
    public String visit (IfLt c) {
        return "if_icmplt " + c.label.toJVM() + "\n";
    }
    public String visit (IfGt c) {
        return "if_icmpgt " + c.label.toJVM() + "\n";
    }
    public String visit (IfLe c) {
        return "if_icmple " + c.label.toJVM() + "\n";
    }
    public String visit (IfGe c) {
        return "if_icmpge " + c.label.toJVM() + "\n";
    }
    public String visit (IfEq c) {
        return "if_icmpeq " + c.label.toJVM() + "\n";
    }
    public String visit (IfNEq c) {
        return "if_icmpne " + c.label.toJVM() + "\n";
    }
    public String visit (Add c) {
        return "iadd\n";
    }
    public String visit (Mul c) {
        return "imul\n";
    }
    public String visit (Div c) {
        return "idiv\n";
    }
    public String visit (Minus c) {
        return "isub\n";
    }
    public String visit (Dup c) {
        return "dup\n";
    }
    public String visit (Nop c) {
        return "nop\n";
    }
}
