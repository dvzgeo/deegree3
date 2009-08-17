//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/

package org.deegree.protocol.wfs.transaction;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.deegree.commons.types.ows.Version.parseVersion;
import static org.deegree.commons.xml.stax.StAXParsingHelper.getAttributeValue;
import static org.deegree.commons.xml.stax.StAXParsingHelper.getRequiredAttributeValue;
import static org.deegree.commons.xml.stax.StAXParsingHelper.requireNextTag;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_110;
import static org.deegree.protocol.wfs.WFSConstants.WFS_NS;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.deegree.commons.types.ows.Version;
import org.deegree.commons.utils.kvp.InvalidParameterValueException;
import org.deegree.commons.utils.kvp.MissingParameterException;
import org.deegree.commons.xml.CommonNamespaces;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.commons.xml.stax.StAXParsingHelper;
import org.deegree.filter.Filter;
import org.deegree.filter.xml.Filter110XMLDecoder;
import org.deegree.protocol.i18n.Messages;
import org.deegree.protocol.wfs.WFSConstants;
import org.deegree.protocol.wfs.transaction.Transaction.ReleaseAction;

/**
 * Adapter between XML encoded <code>Transaction</code> requests and {@link Transaction} objects.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class TransactionXMLAdapter {

    /**
     * Parses a WFS <code>Transaction</code> document into a {@link Transaction} request.
     * <p>
     * Supported versions:
     * <ul>
     * <li>WFS 1.1.0</li>
     * </ul>
     * 
     * @return parsed {@link Transaction} request
     * @throws XMLStreamException
     * @throws XMLParsingException
     *             if a syntax error occurs in the XML
     * @throws MissingParameterException
     *             if the request version is unsupported
     * @throws InvalidParameterValueException
     *             if a parameter contains a syntax error
     */
    public static Transaction parse( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        Version version = parseVersion( getRequiredAttributeValue( xmlStream, "version" ) );

        Transaction result = null;
        if ( VERSION_110.equals( version ) ) {
            result = parse110( xmlStream );
        } else {
            String msg = Messages.get( "UNSUPPORTED_VERSION", version, Version.getVersionsString( VERSION_110 ) );
            throw new InvalidParameterValueException( msg );
        }
        return result;
    }

    /**
     * Parses a WFS 1.1.0 <code>Transaction</code> document into a {@link Transaction} request.
     * 
     * @return parsed {@link Transaction} request
     * @throws XMLStreamException
     * @throws XMLParsingException
     *             if a syntax error occurs in the XML
     * @throws InvalidParameterValueException
     *             if a parameter contains a syntax error
     */
    public static Transaction parse110( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        xmlStream.require( START_ELEMENT, WFS_NS, "Transaction" );

        // optional: '@handle'
        String handle = getAttributeValue( xmlStream, "handle" );

        // optional: '@releaseAction'
        ReleaseAction releaseAction = null;

        // optional: 'wfs:LockId'
        String lockId = null;
        requireNextTag( xmlStream, START_ELEMENT );
        if ( xmlStream.getName().equals( new QName( WFSConstants.WFS_NS, "LockId" ) ) ) {
            lockId = xmlStream.getElementText().trim();
            requireNextTag( xmlStream, START_ELEMENT );
        }

        LazyOperationsIterable iterable = new LazyOperationsIterable( VERSION_110, xmlStream );
        return new Transaction( VERSION_110, handle, lockId, releaseAction, iterable );
    }

    static TransactionOperation parseOperation110( XMLStreamReader xmlStream )
                            throws XMLParsingException, XMLStreamException {

        System.out.println (xmlStream.isStartElement());
        
        if ( !WFS_NS.equals( xmlStream.getNamespaceURI() ) ) {
            String msg = "Unexpected element: " + xmlStream.getName()
                         + "' is not a WFS 1.1.0 operation element. Not in the wfs namespace.";
            throw new XMLParsingException( xmlStream, msg );
        }

        TransactionOperation operation = null;
        String localName = xmlStream.getLocalName();
        if ( "Delete".equals( localName) ) {
            operation = parseDelete110( xmlStream );
        } else if ( "Insert".equals( localName)) {
            operation = parseInsert110( xmlStream );
        } else if ( "Native".equals( localName)) {
            operation = parseNative110( xmlStream );
        } else if ( "Update".equals( localName)) {
            operation = parseUpdate110( xmlStream );
        }
        return operation;
    }

    /**
     * Returns the object representation of a <code>wfs:Delete</code> element. Consumes all corresponding events from
     * the given <code>XMLStream</code>.
     * 
     * @param xmlStream
     *            cursor must point at the <code>START_ELEMENT</code> event (&lt;wfs:Delete&gt;), points at the
     *            corresponding <code>END_ELEMENT</code> event (&lt;/wfs:Delete&gt;) afterwards
     * @return corresponding {@link Delete} object
     * @throws XMLStreamException
     * @throws XMLParsingException
     */
    static Delete parseDelete110( XMLStreamReader xmlStream )
                            throws XMLStreamException {
    
        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );
    
        // required: '@typeName'
        QName ftName = StAXParsingHelper.getAttributeValueAsQName( xmlStream, null, "typeName" );
    
        // required: 'ogc:Filter'
        xmlStream.nextTag();
        xmlStream.require( START_ELEMENT, CommonNamespaces.OGCNS, "Filter" );
        Filter filter = Filter110XMLDecoder.parse( xmlStream );
        xmlStream.require( END_ELEMENT, CommonNamespaces.OGCNS, "Filter" );
        return new Delete( handle, ftName, filter );
    }

    static Insert parseInsert110( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        // optional: '@idGen'
        String idGenString = xmlStream.getAttributeValue( null, "idgen" );

        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );

        // optional: '@inputFormat'
        String inputFormat = xmlStream.getAttributeValue( null, "inputFormat" );

        // optional: '@srsName'
        String srsName = xmlStream.getAttributeValue( null, "srsName" );

        if ( xmlStream.nextTag() != START_ELEMENT ) {

        }
        return null;
    }

    static Update parseUpdate110( XMLStreamReader xmlStream ) {

        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );

        return null;
    }

    static Native parseNative110( XMLStreamReader xmlStream ) {
        return null;
    }
}
