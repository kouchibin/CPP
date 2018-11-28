public class ReturnException extends RuntimeException {
    public final Value returnValue;
    public ReturnException(Value v) {
        returnValue = v;
    }
}
