package net.sf.saxon.expr;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.Value;

import java.io.PrintStream;
import java.util.Iterator;

/**
* Binary Expression: a numeric or boolean expression consisting of the
* two operands and an operator
*/

public abstract class BinaryExpression extends ComputedExpression {

    protected Expression operand0;
    protected Expression operand1;
    protected int operator;       // represented by the token number from class Tokenizer

    /**
    * Create a binary expression identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.AND)
    * @param p1 the right-hand operand
    */

    public BinaryExpression(Expression p0, int op, Expression p1) {
        this.operator = op;
        operand0 = p0;
        operand1 = p1;
        adoptChildExpression(p0);
        adoptChildExpression(p1);
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand0 = operand0.simplify(env);
        operand1 = operand1.simplify(env);
        return this;
    }

    /**
    * Type-check the expression. Default implementation for binary operators that accept
    * any kind of operand
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand0 = operand0.analyze(env, contextItemType);
        operand1 = operand1.analyze(env, contextItemType);
        // if both operands are known, pre-evaluate the expression
        try {
            if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
                return ExpressionTool.eagerEvaluate(this, null);
            }
        } catch (DynamicError err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                operand0 = operand0.promote(offer);
                operand1 = operand1.promote(offer);
            }
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return new PairIterator(operand0, operand1);
    }

    /**
     * Get the operator
     */

    public int getOperator() {
        return operator;
    }

    /**
     * Get the operands
     */

    public Expression[] getOperands() {
        Expression[] ops = {operand0, operand1};
        return ops;
    }

    /**
    * Determine the static cardinality. Default implementation returns [0..1] if either operand
     * can be empty, or [1..1] otherwise.
    */

    public int computeCardinality() {
        if (Cardinality.allowsZero(operand0.getCardinality()) ||
                Cardinality.allowsZero(operand1.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}. This is overridden
     * for some subclasses.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    protected static boolean isCommutative(int operator) {
        return (operator == Token.AND ||
                operator == Token.OR ||
                operator == Token.UNION ||
                operator == Token.INTERSECT ||
                operator == Token.PLUS ||
                operator == Token.MULT ||
                operator == Token.EQUALS ||
                operator == Token.FEQ ||
                operator == Token.NE ||
                operator == Token.FNE
                );
    }

    /**
     * Test if one operator is the inverse of another, so that (A op1 B) is
     * equivalent to (B op2 A). Commutative operators are the inverse of themselves
     * and are therefore not listed here.
     * @param op1 the first operator
     * @param op2 the second operator
     * @return true if the operators are the inverse of each other
     */
    protected static boolean isInverse(int op1, int op2) {
        return op1 != op2 && op1 == Token.inverse(op2);
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        if (other instanceof BinaryExpression) {
            BinaryExpression b = (BinaryExpression)other;
            if (operator == b.operator) {
                if (operand0.equals(b.operand0) &&
                        operand1.equals(b.operand1)) {
                    return true;
                }
                if (isCommutative(operator) &&
                        operand0.equals(b.operand1) &&
                        operand1.equals(b.operand0)) {
                    return true;
                }
            }
            if (isInverse(operator, b.operator) &&
                    operand0.equals(b.operand1) &&
                    operand1.equals(b.operand0)) {
                return true;
            }
            // TODO: recognize associative operators (A|(B|C)) == ((A|B)|C)
        }
        return false;
    }

    /**
    * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
    */

    public int hashCode() {
        // Ensure that an operator and its inverse get the same hash code,
        // so that (A lt B) has the same hash code as (B gt A)
        int op = Math.min(operator, Token.inverse(operator));
        return ("BinaryExpression " + op).hashCode()
                ^ operand0.hashCode()
                ^ operand1.hashCode();
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "operator " + displayOperator());
        operand0.display(level+1, pool, out);
        operand1.display(level+1, pool, out);
    }

    protected String displayOperator() {
        return Token.tokens[operator];
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
