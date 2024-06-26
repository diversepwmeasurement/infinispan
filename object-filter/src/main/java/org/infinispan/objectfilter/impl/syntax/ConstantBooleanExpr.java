package org.infinispan.objectfilter.impl.syntax;

/**
 * A constant boolean expression (tautology or contradiction).
 *
 * @author anistor@redhat.com
 * @since 7.0
 */
public final class ConstantBooleanExpr implements PrimaryPredicateExpr {

   public static final ConstantBooleanExpr TRUE = new ConstantBooleanExpr(true);

   public static final ConstantBooleanExpr FALSE = new ConstantBooleanExpr(false);

   private final boolean constantValue;

   public static ConstantBooleanExpr forBoolean(boolean value) {
      return value ? TRUE : FALSE;
   }

   private ConstantBooleanExpr(boolean constantValue) {
      this.constantValue = constantValue;
   }

   @Override
   public ValueExpr getChild() {
      return null;
   }

   public boolean getValue() {
      return constantValue;
   }

   public ConstantBooleanExpr negate() {
      return constantValue ? FALSE : TRUE;
   }

   @Override
   public <T> T acceptVisitor(Visitor<?, ?> visitor) {
      return (T) visitor.visit(this);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConstantBooleanExpr other = (ConstantBooleanExpr) o;
      return constantValue == other.constantValue;
   }

   @Override
   public int hashCode() {
      return constantValue ? 1 : 0;
   }

   @Override
   public String toString() {
      return constantValue ? "CONST_TRUE" : "CONST_FALSE";
   }

   @Override
   public void appendQueryString(StringBuilder sb) {
      if (constantValue) {
         sb.append("TRUE");
      } else {
         sb.append("FALSE");
      }
   }
}
