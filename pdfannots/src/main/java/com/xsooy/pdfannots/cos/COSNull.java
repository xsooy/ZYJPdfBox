package com.xsooy.pdfannots.cos;

import java.io.IOException;
import java.io.OutputStream;

public final class COSNull extends COSBase
{
    /**
     * The null token.
     */
    public static final byte[] NULL_BYTES = new byte[] {110, 117, 108, 108}; //"null".getBytes( "ISO-8859-1" );

    /**
     * The one null object in the system.
     */
    public static final COSNull NULL = new COSNull();

    /**
     * Constructor.
     */
    private COSNull()
    {
        //limit creation to one instance.
    }

    /**
     * This will output this string as a PDF object.
     *
     * @param output The stream to write to.
     * @throws IOException If there is an error writing to the stream.
     */
    public void writePDF( OutputStream output ) throws IOException
    {
        output.write(NULL_BYTES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "COSNull{}";
    }

}