package net.sf.saxon.trans;

import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.value.Value;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

/**
* XPathException is used to indicate an error in an XPath expression.
* We don't distinguish compile-time errors from run-time errors because there are
* too many overlaps, e.g. constant expressions can be evaluated at compile-time, and
* expressions can be optimised either at compile-time or at run-time.
*/

public abstract class XPathException extends TransformerException {

    // Rewritten in 8.3 to remove the dependency on the javax.xml.QName class, which is not present in JDK 1.4

    private boolean isTypeError = false;
    String errorCodeNamespace;
    String errorCode;
    Value errorObject;
    private boolean hasBeenReported = false;

    public XPathException(String message) {
        super(message);
    }

    public XPathException(Throwable err) {
        super(err);
    }

    public XPathException(String message, Throwable err) {
        super(message, err);
    }

    public XPathException(String message, SourceLocator loc) {
        super(message, loc);
    }

    public XPathException(String message, SourceLocator loc, Throwable err) {
        super(message, loc, err);
    }

    /**
     * Force an exception to a static error
     */

    public StaticError makeStatic() {
        return new StaticError(this);
    }

    public void setIsTypeError(boolean is) {
        isTypeError = is;
    }

    public boolean isTypeError() {
        return isTypeError;
    }

    public void setErrorCode(String code) {
        if (code != null) {
            this.errorCode = code;
            this.errorCodeNamespace = NamespaceConstant.ERR;
        }
    }

    public void setErrorCode(String namespace, String code) {
        this.errorCode = code;
        this.errorCodeNamespace = namespace;
    }

//    public void setErrorCode(QName code) {
//        this.errorCode = code;
//    }

//    public QName getErrorCode() {
//        return errorCode;
//    }

    public String getErrorCodeLocalPart() {
        return errorCode;
    }

    public String getErrorCodeNamespace() {
        return errorCodeNamespace;
    }

    public void setErrorObject(Value value) {
        errorObject = value;
    }

    public Value getErrorObject() {
        return errorObject;
    }
    
    public void setHasBeenReported() {
        hasBeenReported = true;
    }

    public boolean hasBeenReported() {
        return hasBeenReported;
    }

    /**
     * Subclass used to report circularities
     */

    public static class Circularity extends DynamicError {
        public Circularity(String message) {
            super(message);
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
