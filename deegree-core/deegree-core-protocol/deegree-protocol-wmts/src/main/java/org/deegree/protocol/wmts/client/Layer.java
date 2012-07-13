//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2012 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

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
package org.deegree.protocol.wmts.client;

import java.util.List;

public class Layer {

    private final String identifier;

    private final List<Style> styles;

    private final List<String> formats;

    private final List<String> tileMatrixSets;

    public Layer( String identifier, List<Style> styles, List<String> formats, List<String> tileMatrixSets ) {
        this.identifier = identifier;
        this.styles = styles;
        this.formats = formats;
        this.tileMatrixSets = tileMatrixSets;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<Style> getStyles() {
        return styles;
    }

    public List<String> getFormats() {
        return formats;
    }

    public List<String> getTileMatrixSets() {
        return tileMatrixSets;
    }
}
