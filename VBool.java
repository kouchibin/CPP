public class VBool extends Value {
  public final boolean value;
  public VBool (boolean d) { value = d; }
  public boolean equals(Object o) {
    return (o instanceof VBool) && this.value == (((VBool)o).value);
  }
}
