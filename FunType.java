import java.util.LinkedList;
import CPP.Absyn.*;

public class FunType {
    final ListArg args;
    final Type returnType;

    public FunType(Type r, ListArg a) {
        this.returnType = r;
        this.args = a; 
    }
}