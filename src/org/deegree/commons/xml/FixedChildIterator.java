// $HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de


 ---------------------------------------------------------------------------*/
package org.deegree.commons.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.axiom.om.OMElement;
import org.deegree.commons.i18n.Messages;

/**
 * This is a child element iterator that expects a fixed number of child elements. It will throw an
 * {@link XMLProcessingException} when an element is missing, or when you access the last expected element and there
 * are stil child elements left.
 * 
 * @author <a href="mailto:tonnhofer@lat-lon.de">Oliver Tonnhofer</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 * 
 */
public class FixedChildIterator implements Iterable<OMElement>, Iterator<OMElement> {
    private final int expectedChilds;

    private final OMElement parent;

    private final Iterator<?> itr;

    private int i = 0;

    /**
     * @param parent
     * @param expectedChilds
     *            the number of expected child elements
     */
    public FixedChildIterator( OMElement parent, int expectedChilds ) {
        this.parent = parent;
        this.expectedChilds = expectedChilds;
        this.itr = parent.getChildElements();
    }

    public OMElement next() {
        if ( i >= expectedChilds ) { // only allow i next() calls
            throw new NoSuchElementException( "requested to much elements." );
        }
        if ( !itr.hasNext() ) {
            String msg = Messages.getMessage( "XML_PARSING_WRONG_CHILD_COUNT", parent.getQName(), expectedChilds );
            throw new XMLProcessingException( msg );
        }
        OMElement result = (OMElement) itr.next();
        i++;
        if ( i == expectedChilds && itr.hasNext() ) { // more than expected
            String msg = Messages.getMessage( "XML_PARSING_WRONG_CHILD_COUNT", parent.getQName(), expectedChilds );
            throw new XMLProcessingException( msg );
        }
        return result;
    }

    @Override
    public Iterator<OMElement> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return i < expectedChilds;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}