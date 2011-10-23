package net.sf.saxon.instruct;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * This class is a tracing wrapper around any expression: it delivers the same result as the
 * wrapped expression, but traces its evaluation to a TraceListener
 */

public class TraceWrapper extends Instruction {
    Expression child;   // the instruction or other expression to be traced

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception net.sf.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        child = child.simplify(env);
        return this;
    }

    /**                               I
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception net.sf.saxon.trans.XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        child = child.analyze(env, contextItemType);
        return this;
    }

    /**
     * Execute this instruction, with the possibility of returning tail calls if there are any.
     * This outputs the trace information via the registered TraceListener,
     * and invokes the instruction being traced.
     * @param context the dynamic execution context
     * @return either null, or a tail call that the caller must invoke on return
     * @throws net.sf.saxon.trans.XPathException
     */
    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        TraceListener listener = controller.getTraceListener();
    	if (controller.isTracing()) {
   	        listener.enter(getInstructionInfo(), context);
        }
        // Don't attempt tail call optimization when tracing, the results are too confusing
        child.process(context);
   	    if (controller.isTracing()) {
   	        listener.leave(getInstructionInfo());
        }
        return null;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        return child.getItemType();
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *         Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *         Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *         implementation returns ZERO_OR_MORE (which effectively gives no
     *         information).
     */

    public int getCardinality() {
        return child.getCardinality();
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as {@link net.sf.saxon.expr.StaticProperty#DEPENDS_ON_CONTEXT_ITEM} and
     * {@link net.sf.saxon.expr.StaticProperty#DEPENDS_ON_CURRENT_ITEM}. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        return child.getDependencies();
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true unconditionally. This suppresses
     * optimizations, which is appropriate when tracing.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeDependencies() {
        if (child instanceof ComputedExpression) {
            return ((ComputedExpression)child).computeDependencies();
        } else {
            return 0;
        }
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        if (controller.isTracing()) {
            controller.getTraceListener().enter(getInstructionInfo(), context);
        }
        Item result = child.evaluateItem(context);
        if (controller.isTracing()) {
            controller.getTraceListener().leave(getInstructionInfo());
        }
        return result;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        if (controller.isTracing()) {
            controller.getTraceListener().enter(getInstructionInfo(), context);
        }
        SequenceIterator result = child.iterate(context);
        if (controller.isTracing()) {
            controller.getTraceListener().leave(getInstructionInfo());
        }
        return result;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(child);
    }

    public int getInstructionNameCode() {
        if (child instanceof Instruction) {
            return ((Instruction)child).getInstructionNameCode();
        } else {
            return -1;
        }
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        child.display(level, pool, out);
    }
}
