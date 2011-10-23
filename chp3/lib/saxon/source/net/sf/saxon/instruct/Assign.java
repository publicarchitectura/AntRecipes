package net.sf.saxon.instruct;
import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.BindingReference;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Closure;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;
import net.sf.saxon.om.ValueRepresentation;

/**
* saxon:assign element in stylesheet.
*
* The saxon:assign element has mandatory attribute name and optional attribute expr.
* It also allows xsl:extension-element-prefixes etc.
*/

public class Assign extends GeneralVariable implements BindingReference {


    private Binding binding;    // link to the variable declaration

    public Assign() {}

    public void setStaticType(SequenceType type, Value constantValue, int properties) {}

    public void fixup(Binding binding) {
        this.binding = binding;
    }

//    public void setVariableName(String variableName) {}

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.SAXON_ASSIGN;
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        if (binding==null) {
            throw new IllegalStateException("saxon:assign binding has not been fixed up");
        }
        ValueRepresentation value = getSelectValue(context);
        if (value instanceof Closure) {
            value = SequenceExtent.makeSequenceExtent(((Closure)value).iterate(null));
        }
        if (binding instanceof GeneralVariable) {
            if (((GeneralVariable)binding).isGlobal()) {
                context.getController().getBindery().assignGlobalVariable((GlobalVariable)binding, value);
            } else {
                context.setLocalVariable(((GeneralVariable)binding).getSlotNumber(), value);
            }
        } else {

        }
        return null;
    }

    /**
     * Evaluate the variable (method exists only to satisfy the interface)
     */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException();
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
