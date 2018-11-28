public class VInt extends Value {
  public final Integer value;
  public VInt (Integer i) { value = i; }
  public boolean equals(Object o) {
    return (o instanceof VInt) && this.value.equals(((VInt)o).value);
  }
}
