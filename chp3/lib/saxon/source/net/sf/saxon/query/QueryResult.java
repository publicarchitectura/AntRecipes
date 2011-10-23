package net.sf.saxon.query;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.*;
import net.sf.saxon.om.*;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * This utility class takes the result sequence produced by a query, and wraps it as
 * an XML document. The class is never instantiated.
 */
public class QueryResult {

    private static String RESULT_NS = "http://saxon.sf.net/xquery-results";

    private QueryResult() {
    }

    /**
     * Take the results of a query (or any other SequenceIterator) and create
     * an XML document containing copies of all items in the sequence, suitably wrapped
     * @param iterator  The values to be wrapped
     * @param config The Saxon configuration used to evaluate the query
     * @return the document containing the wrapped results
     * @throws XPathException
     */

    public static DocumentInfo wrap(SequenceIterator iterator, Configuration config) throws XPathException {
        NamePool pool = config.getNamePool();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        TinyBuilder builder = new TinyBuilder();

        NamespaceReducer reducer = new NamespaceReducer();
        reducer.setUnderlyingReceiver(builder);
        Receiver tree = reducer;

        tree.setPipelineConfiguration(pipe);
        builder.setPipelineConfiguration(pipe);
        tree.open();
        tree.startDocument(0);

        int resultSequence = pool.allocate("result", RESULT_NS, "sequence");
        int resultDocument = pool.allocate("result", RESULT_NS, "document");
        int resultElement = pool.allocate("result", RESULT_NS, "element");
        int resultAttribute = pool.allocate("result", RESULT_NS, "attribute");
        int resultText = pool.allocate("result", RESULT_NS, "text");
        int resultComment = pool.allocate("result", RESULT_NS, "comment");
        int resultPI = pool.allocate("result", RESULT_NS, "processing-instruction");
        int resultNamespace = pool.allocate("result", RESULT_NS, "namespace");
        int resultAtomicValue = pool.allocate("result", RESULT_NS, "atomic-value");
        int xsiType = pool.allocate("xsi", NamespaceConstant.SCHEMA_INSTANCE, "type");


        tree.startElement(resultSequence, -1, 0, 0);
        tree.namespace(pool.allocateNamespaceCode("result", RESULT_NS), 0);
        tree.namespace(pool.allocateNamespaceCode("xs", NamespaceConstant.SCHEMA), 0);
        tree.namespace(pool.allocateNamespaceCode("xsi", NamespaceConstant.SCHEMA_INSTANCE), 0);
        tree.startContent();

        while (true) {
            Item next = iterator.next();
            if (next == null) {
                break;
            }
            if (next instanceof NodeInfo) {
                switch (((NodeInfo)next).getNodeKind()) {
                    case Type.DOCUMENT:
                        tree.startElement(resultDocument, -1, 0, 0);
                        tree.startContent();
                        ((DocumentInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.endElement();
                        break;
                    case Type.ELEMENT:
                        tree.startElement(resultElement, -1, 0, 0);
                        tree.startContent();
                        ((NodeInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.endElement();
                        break;
                    case Type.ATTRIBUTE:
                        tree.startElement(resultAttribute, -1, 0, 0);
                        ((NodeInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.startContent();
                        tree.endElement();
                        break;
                    case Type.TEXT:
                        tree.startElement(resultText, -1, 0, 0);
                        tree.startContent();
                        ((NodeInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.endElement();
                        break;
                    case Type.COMMENT:
                        tree.startElement(resultComment, -1, 0, 0);
                        tree.startContent();
                        ((NodeInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.endElement();
                        break;
                    case Type.PROCESSING_INSTRUCTION:
                        tree.startElement(resultPI, -1, 0, 0);
                        tree.startContent();
                        ((NodeInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.endElement();
                        break;
                    case Type.NAMESPACE:
                        tree.startElement(resultNamespace, -1, 0, 0);
                        ((NodeInfo)next).copy(tree, NodeInfo.ALL_NAMESPACES, true, 0);
                        tree.startContent();
                        tree.endElement();
                        break;
                }
            } else {
                // display an atomic value
                tree.startElement(resultAtomicValue, -1, 0, 0);
                AtomicType type = (AtomicType)((AtomicValue)next).getItemType();
                int nameCode = type.getNameCode();
                String prefix = pool.getPrefix(nameCode);
                String localName = pool.getLocalName(nameCode);
                String uri = pool.getURI(nameCode);
                if (prefix.equals("")) {
                    prefix = pool.suggestPrefixForURI(uri);
                    if (prefix == null) {
                        prefix = "p" + uri.hashCode();
                    }
                }
                int nscode = pool.allocateNamespaceCode(prefix, uri);
                String displayName = prefix + ':' + localName;
                tree.namespace(nscode, 0);
                tree.attribute(xsiType, -1, displayName, 0, 0);
                tree.startContent();
                tree.characters(next.getStringValue(), 0, 0);
                tree.endElement();
            }

        }
        tree.endElement();
        tree.endDocument();
        tree.close();
        return (DocumentInfo)builder.getCurrentRoot();
    }

    /**
     * Serialize a document containing wrapped query results (or any other document, in fact)
     * as XML.
     * @param node                      The document or element to be serialized
     * @param destination               The Result object to contain the serialized form
     * @param outputProperties          Serialization options
     * @param config                    The Configuration
     * @throws XPathException     If serialization fails
     */

    public static void serialize(NodeInfo node, Result destination, Properties outputProperties, Configuration config)
    throws XPathException {
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        int type = node.getNodeKind();
        if (type==Type.DOCUMENT || type==Type.ELEMENT) {
            DocumentSender sender = new DocumentSender(node);
            Receiver receiver =
                    ResultWrapper.getReceiver(destination,
                                              pipe,
                                              outputProperties);
            NamespaceReducer reducer = new NamespaceReducer();
            reducer.setUnderlyingReceiver(receiver);
            reducer.setPipelineConfiguration(pipe);
            sender.send(reducer);
        } else {
            throw new DynamicError("Node to be serialized must be a Document or Element node");
        }
    }

    /**
     * Serialize an arbitrary sequence, without any special wrapping.
     * @param results the sequence to be serialized
     * @param config the configuration (gives access to information such as the NamePool)
     * @param destination the output stream to which the output is to be written
     * @param outputProps a set of serialization properties as defined in JAXP
     * @throws XPathException if any failure occurs
     */

    public static void serializeSequence(SequenceIterator results, Configuration config,
                                          OutputStream destination, Properties outputProps)
            throws XPathException {
        String encoding = outputProps.getProperty(OutputKeys.ENCODING);
        if (encoding==null) {
            encoding = "UTF-8";
        }
        PrintWriter writer;
        try {
            writer = new PrintWriter(
                new OutputStreamWriter(destination, encoding));
        } catch (UnsupportedEncodingException err) {
            throw new DynamicError(err);
        }
        while (true) {
            Item item = results.next();
            if (item == null) break;
            if (item instanceof NodeInfo) {
                switch (((NodeInfo)item).getNodeKind()) {
                case Type.DOCUMENT:
                case Type.ELEMENT:
                    serialize((NodeInfo)item,
                            new StreamResult(writer),
                            outputProps,
                            config);
                    writer.println("");
                    break;
                case Type.ATTRIBUTE:
                    writer.println(((NodeInfo)item).getLocalPart() +
                                   "=\"" +
                                   item.getStringValue() +
                                   '\"');
                    break;
                case Type.COMMENT:
                    writer.println("<!--" + item.getStringValue() + "-->");
                    break;
                case Type.PROCESSING_INSTRUCTION:
                    writer.println("<?" +
                                   ((NodeInfo)item).getLocalPart() +
                                   ' ' +
                                   item.getStringValue() +
                                   "?>");
                    break;
                default:
                    writer.println(item.getStringValue());
                }
            } else if (item instanceof QNameValue) {
                writer.println(((QNameValue)item).getClarkName());
            } else {
                writer.println(item.getStringValue());
            }
        }
        writer.flush();
    }

//    public static void main(String[] params) throws Exception {
//        StaticQueryContext env = new StaticQueryContext(new Configuration());
//        QueryProcessor qp = new QueryProcessor(env);
//        XQueryExpression exp = qp.compileQuery("<a><b/></a>");
//        SequenceIterator iter = exp.iterator(new DynamicQueryContext());
//        NodeInfo node = (NodeInfo)iter.next();
//        serialize(node, new SAXResult(new ExampleContentHandler()), new Properties());
//
//    }
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
