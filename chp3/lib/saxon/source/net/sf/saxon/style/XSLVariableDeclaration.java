package net.sf.saxon.style;
import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.BindingReference;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

import javax.xml.transform.TransformerConfigurationException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
* Generic class for xsl:variable and xsl:param elements. <br>
*/

public abstract class XSLVariableDeclaration
        extends XSLGeneralVariable
        implements VariableDeclaration, StylesheetProcedure {

    private int slotNumber;

    // List of VariableReference objects that reference this XSLVariableDeclaration
    List references = new ArrayList(10);

    /**
     * Get the SlotManager associated with this stylesheet construct. The SlotManager contains the
     * information needed to manage the local stack frames used by run-time instances of the code.
     * @return the associated SlotManager object
     */

    public SlotManager getSlotManager() {
        return slotManager;
    }


    public int getSlotNumber() {
        return slotNumber;
    }

    /**
    * Get the static type of the variable.
    */

    public abstract SequenceType getRequiredType();

    /**
    * Method called by VariableReference to register the variable reference for
    * subsequent fixup
    */

    public void registerReference(BindingReference ref) {
        references.add(ref);
    }

    /**
     * Get the list of references to this variable or parameter. The items in the list are
     * of class BindingReference.
     */

    public List getReferences() {
        return references;
    }

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction (well, it can be, anyway)
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Get the list of references
     */

    public List getReferenceList() {
        return references;
    }

    /**
    * Notify all references to this variable of the data type
    */

    public void fixupReferences() throws TransformerConfigurationException {
        SequenceType type = getRequiredType();
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            Value constantValue = null;
            int properties = 0;
            if (this instanceof XSLVariable && !isAssignable()) {
                if (select instanceof Value) {
                    // we can't rely on the constant value because it hasn't yet been type-checked,
                    // which could change it (eg by numeric promotion). Rather than attempt all the type-checking
                    // now, we do a quick check. See test bug64
                    int relation = Type.relationship(select.getItemType(), type.getPrimaryType());
                    if (relation == Type.SAME_TYPE || relation == Type.SUBSUMED_BY) {
                        constantValue = (Value)select;
                    }
                }
                if (select != null) {
                    properties = select.getSpecialProperties();
                }
            }
            ((BindingReference)iter.next()).setStaticType(type, constantValue, properties);
        }
        super.fixupReferences();
    }

    /**
    * Check that the variable is not already declared, and allocate a slot number
    */

    public void validate() throws TransformerConfigurationException {
        super.validate();
        if (global) {
            if (!redundant) {
                slotNumber = getPrincipalStylesheet().allocateGlobalSlot(getVariableFingerprint());
            }
        } else {
            checkWithinTemplate();
            SlotManager p = getContainingSlotManager();
            if (p==null) {
                compileError("Local variable must be declared within a template or function", "XT0010");
            } else {
                slotNumber = p.allocateSlotNumber(getVariableFingerprint());
            }
        }
        // Check for duplication
            // Global variables are checked at the XSLStylesheet level
            // For local variables, duplicates are now allowed
            // For parameters, XSLParam and XSLWithParam do their own checks
            // checkDuplicateDeclaration();
    }

    /**
    * Notify all variable references of the Binding instruction
    */

    protected void fixupBinding(Binding binding) {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            ((BindingReference)iter.next()).fixup(binding);
        }
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
