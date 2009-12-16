//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
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

package org.deegree.coverage.raster.io.grid;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.Set;

import org.deegree.commons.utils.FileUtils;
import org.deegree.coverage.raster.AbstractRaster;
import org.deegree.coverage.raster.SimpleRaster;
import org.deegree.coverage.raster.data.RasterDataFactory;
import org.deegree.coverage.raster.data.info.RasterDataInfo;
import org.deegree.coverage.raster.data.nio.ByteBufferRasterData;
import org.deegree.coverage.raster.geom.RasterGeoReference;
import org.deegree.coverage.raster.geom.RasterRect;
import org.deegree.coverage.raster.geom.RasterGeoReference.OriginLocation;
import org.deegree.coverage.raster.io.RasterIOOptions;
import org.deegree.coverage.raster.io.RasterWriter;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.GeometryFactory;
import org.slf4j.Logger;

/**
 * The <code>GridWriter</code> class TODO add class documentation here.
 * 
 * @author <a href="mailto:bezema@lat-lon.de">Rutger Bezema</a>
 * @author last edited by: $Author$
 * @version $Revision$, $Date$
 * 
 */
public class GridWriter implements RasterWriter {
    private static final Logger LOG = getLogger( GridWriter.class );

    /** Defining the number of columns of the grid rasterwriter, to be used in the RasterIOOptions */
    public final static String RASTERIO_COLUMNS = "grid_writer_columns";

    /** Defining the number of rows of the grid rasterwriter, to be used in the RasterIOOptions */
    public final static String RASTERIO_ROWS = "grid_writer_rows";

    private final static GeometryFactory geomFac = new GeometryFactory();

    private final static int DEFAULT_RASTER_TILE_WIDTH = 512;

    private int columns;

    private int rows;

    private Envelope envelope;

    private RasterGeoReference geoRef;

    /* world coordinates */
    private double tileWidth;

    /* world coordinates */
    private double tileHeight;

    private int tileRasterWidth;

    private int tileRasterHeight;

    // private FileChannel readAccess;
    //
    // private FileChannel writeAccess;

    private FileInputStream readStream;

    private FileOutputStream writeStream;

    private File gridFile;

    private RasterDataInfo dataInfo;

    private ByteBufferRasterData tileData;

    private int tilesInFile;

    private int bytesPerTile;

    private boolean leaveStreamOpen;

    /**
     * An empty constructor used in the {@link GridRasterIOProvider}, to a location in time where no information is
     * known yet.
     */
    GridWriter() {
        // empty constructor, no values are known yet (GridRasterIOProvider).
    }

    /**
     * Create a gridfile writer with the given parameters.
     * 
     * @param targetColumns
     *            the number of tiles in the width of the file (columns)
     * @param targetRows
     *            the number of tiles in the height of the file (rows)
     * @param rasterEnvelope
     *            the Envelope of the total grid file.
     * @param geoRef
     *            the geo reference of the grid file.
     * @param gridFile
     *            write to the given file.
     * @param dataInfo
     *            information about the data written to the grid file.
     * @throws IOException
     */
    public GridWriter( int targetColumns, int targetRows, Envelope rasterEnvelope, RasterGeoReference geoRef,
                       File gridFile, RasterDataInfo dataInfo ) throws IOException {
        instantiate( targetColumns, targetRows, rasterEnvelope, geoRef, gridFile, dataInfo );
    }

    private synchronized void instantiate( int targetColumns, int targetRows, Envelope rasterEnvelope,
                                           RasterGeoReference geoRef, File gridFile, RasterDataInfo dataInfo )
                            throws IOException {
        this.envelope = rasterEnvelope;
        this.columns = targetColumns;
        this.rows = targetRows;
        this.geoRef = geoRef.createRelocatedReference( OriginLocation.OUTER );
        tileWidth = this.envelope.getSpan0() / columns;
        tileHeight = this.envelope.getSpan1() / rows;
        double[] origin = geoRef.getOrigin();

        int[] rasterCoordinate = geoRef.getRasterCoordinate( origin[0] + Math.ceil( tileWidth ),
                                                             origin[1] - Math.ceil( tileHeight ) );
        this.tileRasterWidth = rasterCoordinate[0];
        this.tileRasterHeight = rasterCoordinate[1];

        this.gridFile = gridFile;
        if ( this.gridFile != null && !this.gridFile.exists() ) {
            this.gridFile.createNewFile();
        }
        this.dataInfo = dataInfo;
        // this tile data does not need to be cached.
        this.tileData = RasterDataFactory.createRasterData( tileRasterWidth, tileRasterHeight, dataInfo.bandInfo,
                                                            dataInfo.dataType, dataInfo.interleaveType, false );
        this.bytesPerTile = this.tileRasterWidth * this.tileRasterHeight * dataInfo.bands * dataInfo.dataSize;
        this.tilesInFile = columns * rows;
    }

    /**
     * @param gridFile
     * @param options
     * @throws IOException
     */
    private void instantiate( AbstractRaster raster, File gridFile, RasterIOOptions options )
                            throws IOException {
        Envelope env = raster.getEnvelope();
        RasterGeoReference geoRef = raster.getRasterReference();
        int targetColumns = 0;
        int targetRows = 0;
        if ( options != null ) {
            String op = options.get( RASTERIO_COLUMNS );
            if ( op != null ) {
                try {
                    targetColumns = Integer.parseInt( op );
                } catch ( NumberFormatException e ) {
                    // calc columns
                }
            }
            op = options.get( RASTERIO_ROWS );
            if ( op != null ) {
                try {
                    targetRows = Integer.parseInt( op );
                } catch ( NumberFormatException e ) {
                    // calc rows
                }
            }
        }
        if ( targetColumns <= 0 || targetRows <= 0 ) {
            RasterRect rr = geoRef.convertEnvelopeToRasterCRS( env );
            if ( targetColumns <= 0 ) {
                targetColumns = (int) Math.max( 1, Math.ceil( rr.width / (double) DEFAULT_RASTER_TILE_WIDTH ) );
            }
            if ( targetRows <= 0 ) {
                targetRows = (int) Math.max( 1, Math.ceil( rr.height / (double) DEFAULT_RASTER_TILE_WIDTH ) );
            }
        }

        instantiate( targetColumns, targetRows, raster.getEnvelope(), raster.getRasterReference(), gridFile,
                     raster.getRasterDataInfo() );
    }

    @Override
    public boolean canWrite( AbstractRaster raster, RasterIOOptions options ) {
        return raster != null;
    }

    @Override
    public Set<String> getSupportedFormats() {
        return new HashSet<String>( GridRasterIOProvider.FORMATS );
    }

    @Override
    public void write( AbstractRaster raster, File gridFile, RasterIOOptions options )
                            throws IOException {
        if ( gridFile == null ) {
            throw new IOException( "No grid file given." );
        }
        if ( envelope == null
             || ( this.gridFile != null && !this.gridFile.getAbsoluteFile().equals( gridFile.getAbsoluteFile() ) ) ) {
            instantiate( raster, gridFile, options );
        }
        write( raster, options );
    }

    @Override
    public void write( AbstractRaster raster, OutputStream out, RasterIOOptions options )
                            throws IOException {
        throw new UnsupportedOperationException( "Ouputing to streams is not supported." );
    }

    /**
     * Write the given raster to the previously defined gridfile.
     * 
     * @param raster
     *            to write
     * @param options
     *            can hold information about the info file etc.
     * @throws IOException
     */
    public void write( AbstractRaster raster, RasterIOOptions options )
                            throws IOException {

        Envelope env = raster.getEnvelope();
        int minColumn = getColumn( env.getMin().get0() );
        int minRow = getRow( env.getMax().get1() );
        int maxColumn = getColumn( env.getMax().get0() );
        int maxRow = getRow( env.getMin().get1() );
        if ( ( maxColumn == -1 ) || ( maxRow == -1 ) || ( minColumn == columns ) || ( minRow == rows ) ) {
            throw new IOException( "The given raster is outside the envelope." );
        }
        // reset values to maximal/minimal allowed
        minColumn = max( minColumn, 0 );
        minRow = max( minRow, 0 );
        maxColumn = min( maxColumn, columns - 1 );
        maxRow = min( maxRow, rows - 1 );
        for ( int row = minRow; row <= maxRow; row++ ) {
            for ( int column = minColumn; column <= maxColumn; column++ ) {
                write( raster, column, row );
            }
        }
        writeMetadataFile( options );
    }

    /**
     * Writes the metadata file for this grid file.
     * 
     * @param options
     * @throws IOException
     */
    public void writeMetadataFile( RasterIOOptions options )
                            throws IOException {
        File metaInfo = null;
        if ( gridFile != null ) {
            metaInfo = GridMetaInfoFile.fileNameFromOptions( gridFile.getParent(), FileUtils.getFilename( gridFile ),
                                                             options );
        } else {
            throw new IOException( "No gridfile specified, could not write the info file" );
        }
        GridMetaInfoFile.writeToFile( metaInfo, new GridMetaInfoFile( this.geoRef, this.rows, this.columns,
                                                                      this.tileRasterWidth, this.tileRasterHeight,
                                                                      this.dataInfo ), options );
    }

    private final FileChannel getReadChannel()
                            throws IOException {
        synchronized ( tileData ) {
            if ( this.readStream == null ) {
                if ( !gridFile.exists() ) {
                    // the file was deleted
                    gridFile.createNewFile();
                }
                this.readStream = new FileInputStream( gridFile );
            }
            return readStream.getChannel();
        }
    }

    private final synchronized FileChannel getWriteChannel()
                            throws IOException {
        synchronized ( tileData ) {
            if ( this.writeStream == null ) {
                if ( !gridFile.exists() ) {
                    // the file was deleted
                    gridFile.createNewFile();
                }
                this.writeStream = new FileOutputStream( gridFile );
            }
            return writeStream.getChannel();
        }
    }

    /**
     * Signals the gridfile reader that it should (not) close the stream after a read.
     * 
     * @param yesNo
     */
    protected void leaveStreamOpen( boolean yesNo ) {
        this.leaveStreamOpen = yesNo;
        synchronized ( tileData ) {
            if ( !this.leaveStreamOpen ) {
                try {
                    closeWriteStream();
                } catch ( IOException e ) {
                    LOG.debug( "Could not close stream because: {}", e.getLocalizedMessage(), e );
                }
            }
        }
    }

    private final void closeWriteStream()
                            throws IOException {
        synchronized ( tileData ) {
            if ( this.writeStream != null && !this.leaveStreamOpen ) {
                this.writeStream.close();
                this.writeStream = null;
            }
        }
    }

    private final void closeReadStream()
                            throws IOException {
        synchronized ( tileData ) {
            if ( this.readStream != null && !this.leaveStreamOpen ) {
                this.readStream.close();
                this.readStream = null;
            }
        }
    }

    private int calcFilePosition( int column, int row ) {
        int tileId = getTileId( column, row );
        int tileInBlob = tileId % tilesInFile;
        return tileInBlob * bytesPerTile;
    }

    private ByteBufferRasterData readData( int column, int row )
                            throws IOException {
        synchronized ( tileData ) {
            ByteBuffer buffer = tileData.getByteBuffer();
            buffer.rewind();
            int position = calcFilePosition( column, row );
            // transfer the data from the blob
            FileChannel channel = getReadChannel();
            channel.position( position );
            channel.read( buffer );
            closeReadStream();
            buffer.rewind();
            return tileData;
        }

    }

    /**
     * Calculates the envelope for a tile at a given position in the grid.
     * 
     * @param column
     *            column , must be in the range [0 ... #columns - 1]
     * @param row
     *            row , must be in the range [0 ... #rows - 1]
     * @return the tile's envelope
     */
    protected Envelope getTileEnvelope( int column, int row ) {
        double xOffset = column * tileWidth;
        double yOffset = ( rows - row - 1 ) * tileHeight;

        double minX = envelope.getMin().get0() + xOffset;
        double minY = envelope.getMin().get1() + yOffset;
        double maxX = minX + tileWidth;
        double maxY = minY + tileHeight;

        return geomFac.createEnvelope( minX, minY, maxX, maxY, envelope.getCoordinateSystem() );
    }

    private int getColumn( double x ) {
        double dx = x - envelope.getMin().get0();
        int column = (int) Math.floor( ( columns * dx ) / envelope.getSpan0() );
        if ( column < 0 ) {
            // signal outside
            return -1;
        }
        if ( column > columns - 1 ) {
            // signal outside
            return columns;
        }
        return column;
    }

    private int getRow( double y ) {
        double dy = y - envelope.getMin().get1();
        int row = (int) Math.floor( ( ( rows * ( envelope.getSpan1() - dy ) ) / envelope.getSpan1() ) );
        if ( row < 0 ) {
            // signal outside
            return -1;
        }
        if ( row > rows - 1 ) {
            // signal outside
            return rows;
        }
        return row;
    }

    /**
     * Calculates the id for a tile at a given position in the grid.
     * 
     * @param column
     *            column, must be in the range [0 ... #columns - 1]
     * @param row
     *            row, must be in the range [0 ... #rows - 1]
     * @return the tile's id
     */
    protected int getTileId( int column, int row ) {
        int idx = row * columns + column;
        return idx;
    }

    /**
     * @param newBytes
     * @throws IOException
     */
    public void writeEntireFile( ByteBuffer newBytes )
                            throws IOException {
        if ( newBytes.capacity() != ( dataInfo.bands * dataInfo.dataSize * columns * rows ) ) {
            throw new IllegalArgumentException( "byte buffer is to small, required bytes:"
                                                + ( dataInfo.bands * dataInfo.dataSize * columns * rows )
                                                + ", provided bytes: " + newBytes.capacity() );
        }
        synchronized ( tileData ) {
            FileChannel fileChannel = getWriteChannel();
            FileLock lock = fileChannel.lock();
            fileChannel.position( 0 );
            newBytes.rewind();
            fileChannel.write( newBytes );
            lock.release();
            closeWriteStream();
        }
    }

    /**
     * @param row
     * @param column
     * @param tileBuffer
     * @return true if the writing of the tile was successful.
     * @throws IOException
     */
    public boolean writeTile( int column, int row, ByteBuffer tileBuffer )
                            throws IOException {
        if ( tileBuffer == null || tileBuffer.capacity() != this.bytesPerTile ) {
            throw new IllegalArgumentException( "Wrong number of bytes." );
        }
        synchronized ( tileData ) {
            int position = calcFilePosition( column, row );
            FileChannel fileChannel = getWriteChannel();
            FileLock lock = fileChannel.lock( position, position + bytesPerTile, false );
            fileChannel.position( position );
            tileBuffer.rewind();
            fileChannel.write( tileBuffer );
            lock.release();
            closeWriteStream();
        }
        return true;
    }

    /**
     * @param raster
     * @param columnId
     * @param rowId
     * @throws IOException
     */
    private void write( AbstractRaster raster, int column, int row )
                            throws IOException {
        Envelope tileEnvelope = getTileEnvelope( column, row );
        RasterGeoReference tileRasterReference = RasterGeoReference.create( OriginLocation.OUTER, tileEnvelope,
                                                                            tileRasterWidth, tileRasterHeight );
        SimpleRaster subRaster = raster.getSubRaster( tileEnvelope ).getAsSimpleRaster();
        RasterRect newDataPosition = tileRasterReference.convertEnvelopeToRasterCRS( subRaster.getEnvelope() );

        synchronized ( tileData ) {
            // read in the data.
            ByteBufferRasterData fileData = readData( column, row );
            // override the new data with the old data.
            fileData.setSubset( newDataPosition.x, newDataPosition.y, newDataPosition.width, newDataPosition.height,
                                subRaster.getRasterData() );
            // try {
            // RasterFactory.saveRasterToFile( subRaster, new File( "/tmp/subraster_" + row + "," + column + ".png" ) );
            // ImageIO.write( RasterFactory.rasterDataToImage( fileData ), "png", new File( "/tmp/filedata_" + row
            // + "," + column + ".png" ) );
            // } catch ( IOException e ) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }

            int position = calcFilePosition( column, row );
            FileChannel fileChannel = getWriteChannel();
            FileLock lock = fileChannel.lock( position, position + bytesPerTile, false );
            fileChannel.position( position );
            ByteBuffer buffer = fileData.getByteBuffer();
            buffer.rewind();
            fileChannel.write( buffer );
            lock.release();
            closeWriteStream();
        }
    }
}
