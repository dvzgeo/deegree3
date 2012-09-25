//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/deegree3/trunk/deegree-core/deegree-core-metadata/src/main/java/org/deegree/metadata/iso/persistence/ISOMetadataStoreProvider.java $
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
package org.deegree.metadata.iso.persistence.memory;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.deegree.metadata.iso.persistence.memory.GetTestRecordsUtils.getRecord;

import java.util.ArrayList;
import java.util.List;

import org.deegree.commons.tom.primitive.PrimitiveValue;
import org.deegree.commons.xml.CommonNamespaces;
import org.deegree.commons.xml.NamespaceBindings;
import org.deegree.cs.CRSUtils;
import org.deegree.filter.Filter;
import org.deegree.filter.Operator;
import org.deegree.filter.OperatorFilter;
import org.deegree.filter.comparison.PropertyIsBetween;
import org.deegree.filter.comparison.PropertyIsEqualTo;
import org.deegree.filter.expression.Literal;
import org.deegree.filter.expression.ValueReference;
import org.deegree.filter.logical.Or;
import org.deegree.filter.spatial.BBOX;
import org.deegree.geometry.GeometryFactory;
import org.deegree.metadata.iso.ISORecord;
import org.deegree.metadata.persistence.MetadataQuery;
import org.deegree.metadata.persistence.MetadataResultSet;
import org.deegree.protocol.csw.MetadataStoreException;
import org.junit.Test;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz</a>
 * @author last edited by: $Author: lyn $
 * 
 * @version $Revision: 30992 $, $Date: 2011-05-31 16:09:20 +0200 (Di, 31. Mai 2011) $
 */
public class StoredISORecordsTest {

    private static final NamespaceBindings nsContext = CommonNamespaces.getNamespaceContext();

    /*
     * AddRecords
     */

    @Test
    public void testAddRecordTwice()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        int expectedNumberOfRecords = storedIsoRecords.getNumberOfStoredRecords();
        storedIsoRecords.addRecord( getRecord( "1.xml" ) );
        assertEquals( expectedNumberOfRecords, storedIsoRecords.getNumberOfStoredRecords() );
    }

    @Test
    public void testAddRecordWithoutFileIdentifier()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        int expectedNumberOfRecords = storedIsoRecords.getNumberOfStoredRecords();
        storedIsoRecords.addRecord( getRecord( "withoutFileIdentifier.xml" ) );
        assertEquals( expectedNumberOfRecords, storedIsoRecords.getNumberOfStoredRecords() );
    }

    /*
     * GetRecordById
     */

    @Test
    public void testGetRecordById()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        MetadataResultSet<ISORecord> recordResultSet = storedIsoRecords.getRecordById( singletonList( "f90258d9a412aa5f3ba679b4997bb176" ),
                                                                                       null );

        assertTrue( recordResultSet.next() );
        assertNotNull( recordResultSet.getRecord() );
        assertFalse( recordResultSet.next() );
    }

    @Test
    public void testGetRecordByIdWithUnknownId()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        MetadataResultSet<ISORecord> recordResultSet = storedIsoRecords.getRecordById( singletonList( "UNKNOWN_ID" ),
                                                                                       null );

        assertFalse( recordResultSet.next() );
    }

    @Test
    public void testGetRecordByIds()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        List<String> ids = new ArrayList<String>();
        ids.add( "f90258d9a412aa5f3ba679b4997bb176" );
        ids.add( "15c1c1bbe5b4409c2fe10639bb54330f" );
        MetadataResultSet<ISORecord> recordResultSet = storedIsoRecords.getRecordById( ids, null );

        assertTrue( recordResultSet.next() );
        assertNotNull( recordResultSet.getRecord() );
        assertTrue( recordResultSet.next() );
        assertNotNull( recordResultSet.getRecord() );
        assertFalse( recordResultSet.next() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordByIdNullList()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        storedIsoRecords.getRecordById( null, null );
    }

    /*
     * GetRecords
     */

    @Test
    public void testGetRecordsAll()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        MetadataQuery query = new MetadataQuery( null, null, null, null, 1, 100 );
        MetadataResultSet<ISORecord> allRecords = storedIsoRecords.getRecords( query );
        assertEquals( storedIsoRecords.getNumberOfStoredRecords(), allRecords.getRemaining() );
    }

    @Test
    public void testGetRecordsWithLimitedMaxRecord()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        int maxRecords = 2;
        MetadataQuery query = new MetadataQuery( null, null, null, null, 1, maxRecords );
        MetadataResultSet<ISORecord> allRecords = storedIsoRecords.getRecords( query );
        assertEquals( maxRecords, allRecords.getRemaining() );
    }

    @Test
    public void testGetRecordsWithStartPositionWithoutQuery()
                            throws Exception {
        StoredISORecords storedRecords = new StoredISORecords();
        storedRecords.addRecord( getRecord( "1.xml" ) );
        ISORecord expectedRecord = getRecord( "3.xml" );
        storedRecords.addRecord( expectedRecord );
        storedRecords.addRecord( getRecord( "2.xml" ) );

        int maxRecords = 1;
        MetadataQuery query = new MetadataQuery( null, null, null, null, 2, maxRecords );
        MetadataResultSet<ISORecord> records = storedRecords.getRecords( query );
        assertEquals( maxRecords, records.getRemaining() );
        ISORecord actualRecord = records.getRecord();
        assertEquals( expectedRecord.getIdentifier(), actualRecord.getIdentifier() );
    }

    @Test
    public void testGetRecordsWithStartPositionWithQuery()
                            throws Exception {
        StoredISORecords storedRecords = new StoredISORecords();
        storedRecords.addRecord( getRecord( "1.xml" ) );
        // not matched by the filter!
        storedRecords.addRecord( getRecord( "2.xml" ) );
        ISORecord expectedRecord = getRecord( "3.xml" );
        storedRecords.addRecord( expectedRecord );
        Literal<PrimitiveValue> literal = new Literal<PrimitiveValue>( "IKONOS 2" );
        Operator operator = new PropertyIsEqualTo( new ValueReference( "Subject", nsContext ), literal, true, null );

        Filter filter = new OperatorFilter( operator );
        int maxRecords = 1;
        MetadataQuery query = new MetadataQuery( null, null, filter, null, 2, maxRecords );
        MetadataResultSet<ISORecord> records = storedRecords.getRecords( query );
        assertEquals( maxRecords, records.getRemaining() );
        ISORecord actualRecord = records.getRecord();
        assertEquals( expectedRecord.getIdentifier(), actualRecord.getIdentifier() );
    }

    @Test
    public void testGetRecordsOrder()
                            throws Exception {
        StoredISORecords storedRecords = new StoredISORecords();
        ISORecord record1 = getRecord( "3.xml" );
        storedRecords.addRecord( record1 );
        ISORecord record2 = getRecord( "1.xml" );
        storedRecords.addRecord( record2 );
        ISORecord record3 = getRecord( "2.xml" );
        storedRecords.addRecord( record3 );

        MetadataQuery query = new MetadataQuery( null, null, null, null, 1, 100 );
        MetadataResultSet<ISORecord> records = storedRecords.getRecords( query );
        assertSameOrderAsInserted( record1, record2, record3, records );
    }

    private void assertSameOrderAsInserted( ISORecord record1, ISORecord record2, ISORecord record3,
                                            MetadataResultSet<ISORecord> records )
                            throws MetadataStoreException {
        assertEquals( record1.getIdentifier(), records.getRecord().getIdentifier() );
        assertEquals( record2.getIdentifier(), records.getRecord().getIdentifier() );
        assertEquals( record3.getIdentifier(), records.getRecord().getIdentifier() );
    }

    @Test
    public void testGetRecordsAllWithFilterForSubject()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        Literal<PrimitiveValue> literal = new Literal<PrimitiveValue>( "SPOT 2" );
        Operator operator = new PropertyIsEqualTo( new ValueReference( "Subject", nsContext ), literal, true, null );

        Filter filter = new OperatorFilter( operator );
        MetadataQuery query = new MetadataQuery( null, null, filter, null, 1, 100 );
        MetadataResultSet<ISORecord> allRecords = storedIsoRecords.getRecords( query );
        assertEquals( 1, allRecords.getRemaining() );
    }

    @Test
    public void testGetRecordsAllWithFilterForBBox()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        GeometryFactory geomFactory = new GeometryFactory();
        ValueReference reference = new ValueReference( "apiso:BoundingBox", nsContext );
        Operator operator = new BBOX( reference, geomFactory.createEnvelope( 10, 45.22, 11., 45.5, CRSUtils.EPSG_4326 ) );
        Filter filter = new OperatorFilter( operator );
        MetadataQuery query = new MetadataQuery( null, null, filter, null, 1, 100 );
        MetadataResultSet<ISORecord> allRecords = storedIsoRecords.getRecords( query );
        assertEquals( 3, allRecords.getRemaining() );
    }

    @Test
    public void testGetRecordsAllWithComplexFilter()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();

        Literal<PrimitiveValue> literalSubject = new Literal<PrimitiveValue>( "SPOT 2" );
        Operator operatorSubject = new PropertyIsEqualTo( new ValueReference( "Subject", nsContext ), literalSubject,
                                                          true, null );

        Literal<PrimitiveValue> lowerCreation = new Literal<PrimitiveValue>( "2006-06-14" );
        Literal<PrimitiveValue> upperCreation = new Literal<PrimitiveValue>( "2006-06-16" );
        Operator operatorCreation = new PropertyIsBetween( new ValueReference( "apiso:CreationDate", nsContext ),
                                                           lowerCreation, upperCreation, true, null );

        Operator or = new Or( operatorSubject, operatorCreation );
        Filter filter = new OperatorFilter( or );
        MetadataQuery query = new MetadataQuery( null, null, filter, null, 1, 100 );
        MetadataResultSet<ISORecord> allRecords = storedIsoRecords.getRecords( query );
        assertEquals( 4, allRecords.getRemaining() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordsWithNullQuery()
                            throws Exception {
        StoredISORecords storedIsoRecords = getStoredIsoRecords();
        storedIsoRecords.getRecords( null );
    }

    private StoredISORecords getStoredIsoRecords()
                            throws Exception {
        StoredISORecords storedRecords = new StoredISORecords();

        List<ISORecord> allRecords = GetTestRecordsUtils.getAllRecords();
        for ( ISORecord record : allRecords ) {
            storedRecords.addRecord( record );
        }
        return storedRecords;
    }

}
