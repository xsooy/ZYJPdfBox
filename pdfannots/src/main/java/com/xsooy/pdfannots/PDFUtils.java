package com.xsooy.pdfannots;

import android.util.Log;

import com.xsooy.pdfannots.cos.COSArray;
import com.xsooy.pdfannots.cos.COSBase;
import com.xsooy.pdfannots.cos.COSBoolean;
import com.xsooy.pdfannots.cos.COSInteger;
import com.xsooy.pdfannots.cos.COSNull;
import com.xsooy.pdfannots.cos.COSNumber;
import com.xsooy.pdfannots.cos.COSObject;
import com.xsooy.pdfannots.cos.COSObjectKey;
import com.xsooy.pdfannots.random.RandomAccessBufferedFileInputStream;
import com.xsooy.pdfannots.random.RandomAccessRead;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;

public class PDFUtils {

    public RandomAccessRead source;
    private long fileLen;
    private int readTrailBytes = 2048;
    protected static final byte ASCII_LF = 10;
    /**
     * ASCII code for carriage return.
     */
    protected static final byte ASCII_CR = 13;
    private static final byte ASCII_ZERO = 48;
    private static final byte ASCII_NINE = 57;
    private static final byte ASCII_SPACE = 32;

    protected static final char[] EOF_MARKER = new char[] { '%', '%', 'E', 'O', 'F' };
    private static final char[] STARTXREF = new char[] { 's','t','a','r','t','x','r','e','f' };

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    protected static final String ENDOBJ_STRING = "endobj";
    protected static final String ENDSTREAM_STRING = "endstream";

    /*** ISO-8859-1 charset */
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    /*** Windows-1252 charset */
    public static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");

    /*** UTF-8 charset */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private Map<COSObjectKey,COSNumber> xrefMap = new HashMap<>();
    private Map<String, COSBase> trailerMap = new HashMap<>();

    public boolean loadFile(File file) {
        try {
            RandomAccessBufferedFileInputStream raFile = new RandomAccessBufferedFileInputStream(file);
            source = raFile;
            fileLen = source.length();
            //从文件尾部读取，获取startxref起始点
            source.seek(getStartxrefOffset());
            //跳过startxref
            Log.w("ceshi",readString());
            //读取交叉引用表起始点
            long startXrefOffset = Long.parseLong(readString());
            source.seek(startXrefOffset);

            readXref();
            readTrailer();
            //TODO:若存在2个以上的Prev，需做另外的处理
            if (trailerMap.containsKey("Prev")) {
                source.seek(((COSNumber)trailerMap.get("Prev")).longValue());
                readXref();
                readTrailer();
            }
            readObjByName("Root");
            readObjByName("Pages");

            COSNumber count = (COSNumber) trailerMap.get("Count");
            for (int i=0;i<count.intValue();i++) {
                COSArray array = (COSArray)trailerMap.get("Kids");
                COSNumber number = (COSNumber) array.get(i);
                source.seek(number.longValue());
                readLine();
                skipSpaces();
                readObj();
            }

            if (trailerMap.containsKey("Annots")) {
                Log.w("ceshi","包含注释对象");
            }

//            Log.w("ceshi","GsonUtils.toJson(xrefMap):"+GsonUtils.toJson(xrefMap));
//            Log.w("ceshi","GsonUtils.toJson(trailerMap):"+GsonUtils.toJson(trailerMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trailerMap.containsKey("Annots");
    }

    private void readObjByName(String name) throws IOException {
        if (!trailerMap.containsKey(name)){
            return;
        }
        COSNumber number = (COSNumber) trailerMap.get(name);
        assert number != null;
        source.seek(number.longValue());
        readLine();
        skipSpaces();
        readObj();
    }

    private void readObj() throws IOException {
        skipSpaces();
        //读取 <<
        readExpectedChar('<');
        readExpectedChar('<');
        skipSpaces();
        boolean done = false;
        while (!done) {
            char c = (char) source.peek();
            if (c == '/') {

                String name = parseCOSName();
                skipSpaces();
                COSBase value = parseDirObject();
                skipSpaces();
                if (!(value instanceof COSNumber) || !isDigit(source.peek())) {
                    if (!trailerMap.containsKey(name))
                        trailerMap.put(name,value);
                } else {
                    COSBase generationNumber = parseDirObject();
                    skipSpaces();
                    readExpectedChar('R');
                    skipSpaces();
                    COSObjectKey key = new COSObjectKey(((COSInteger) value).longValue(),
                            ((COSInteger) generationNumber).intValue());
                    if (!trailerMap.containsKey(name))
                        trailerMap.put(name,xrefMap.get(key));
                }
            } else if (c == '>') {
                done = true;
                readExpectedChar('>');
                readExpectedChar('>');
            }
        }
    }

    protected COSArray parseCOSArray() throws IOException
    {
        long startPosition = source.getPosition();
        readExpectedChar('[');
        COSArray po = new COSArray();
        COSBase pbo;
        skipSpaces();
        int i;
        while( ((i = source.peek()) > 0) && ((char)i != ']') )
        {
            pbo = parseDirObject();
            if( pbo instanceof COSObject)
            {
                // We have to check if the expected values are there or not PDFBOX-385
                if (po.size() > 0 && po.get(po.size() - 1) instanceof COSInteger)
                {
                    COSInteger genNumber = (COSInteger)po.remove( po.size() -1 );
                    if (po.size() > 0 && po.get(po.size() - 1) instanceof COSInteger)
                    {
                        COSInteger number = (COSInteger)po.remove( po.size() -1 );
                        COSObjectKey key = new COSObjectKey(number.longValue(), genNumber.intValue());
                        pbo = getObjectFromPool(key);
                    }
                    else
                    {
                        // the object reference is somehow wrong
                        pbo = null;
                    }
                }
                else
                {
                    pbo = null;
                }
            }
            if( pbo != null )
            {
                po.add( pbo );
            }
            else
            {
                //it could be a bad object in the array which is just skipped
                Log.w("PdfBox-Android", "Corrupt object reference at offset " +
                        source.getPosition() + ", start offset: " + startPosition);

                // This could also be an "endobj" or "endstream" which means we can assume that
                // the array has ended.
                String isThisTheEnd = readString();
                source.rewind(isThisTheEnd.getBytes(ISO_8859_1).length);
                if(ENDOBJ_STRING.equals(isThisTheEnd) || ENDSTREAM_STRING.equals(isThisTheEnd))
                {
                    return po;
                }
            }
            skipSpaces();
        }
        // read ']'
        source.read();
        skipSpaces();
        return po;
    }

    private COSBase getObjectFromPool(COSObjectKey key) throws IOException
    {
        return xrefMap.get(key);
    }

    private void readTrailer() throws IOException {
        if (readString().equals("trailer")) {
            readObj();
        }
    }

    protected COSBase parseDirObject() throws IOException
    {
        COSBase retval = null;

        skipSpaces();
        int nextByte = source.peek();
        char c = (char)nextByte;
        switch(c)
        {
            case '<':
            {
                // pull off first left bracket
                int leftBracket = source.read();
                // check for second left bracket
                c = (char) source.peek();
                source.rewind(1);
                if(c == '<')
                {
                    readObj();
                    retval = COSNull.NULL;
//                    retval = parseCOSDictionary();
//                    skipSpaces();
                }
                else
                {
//                    retval = parseCOSString();
                    while (source.read()!='>') {

                    }
                    retval = COSNull.NULL;
                }
                break;
            }
            case '[':
            {
                // array
                retval = parseCOSArray();
                break;
            }
            case '(':
//                retval = parseCOSString();
                while (source.read()!=')') {

                }
                retval = COSNull.NULL;
                break;
            case '/':
                // name
                parseCOSName();
                retval = COSNull.NULL;
                break;
            case 'n':
            {
                // null
//                readExpectedString(NULL);
                retval = COSNull.NULL;
                break;
            }
            case 't':
            {
                String trueString = new String( source.readFully(4), ISO_8859_1 );
                if( trueString.equals( TRUE ) )
                {
                    retval = COSBoolean.TRUE;
                }
                else
                {
                    throw new IOException( "expected true actual='" + trueString + "' " + source +
                            "' at offset " + source.getPosition());
                }
                break;
            }
            case 'f':
            {
                String falseString = new String( source.readFully(5), ISO_8859_1 );
                if( falseString.equals( FALSE ) )
                {
                    retval = COSBoolean.FALSE;
                }
                else
                {
                    throw new IOException( "expected false actual='" + falseString + "' " + source +
                            "' at offset " + source.getPosition());
                }
                break;
            }
            case 'R':
                source.read();
                retval = new COSObject(null);
                break;
            case (char)-1:
                return null;
            default:
            {
                if( Character.isDigit(c) || c == '-' || c == '+' || c == '.')
                {
                    StringBuilder buf = new StringBuilder();
                    int ic = source.read();
                    c = (char)ic;
                    while( Character.isDigit( c )||
                            c == '-' ||
                            c == '+' ||
                            c == '.' ||
                            c == 'E' ||
                            c == 'e' )
                    {
                        buf.append( c );
                        ic = source.read();
                        c = (char)ic;
                    }
                    if( ic != -1 )
                    {
                        source.rewind(1);
                    }
                    retval = COSNumber.get( buf.toString() );
                }
                else
                {
                    //This is not suppose to happen, but we will allow for it
                    //so we are more compatible with POS writers that don't
                    //follow the spec
                    String badString = readString();
                    if (badString.isEmpty())
                    {
                        int peek = source.peek();
                        // we can end up in an infinite loop otherwise
                        throw new IOException( "Unknown dir object c='" + c +
                                "' cInt=" + (int)c + " peek='" + (char)peek
                                + "' peekInt=" + peek + " at offset " + source.getPosition() );
                    }

                    // if it's an endstream/endobj, we want to put it back so the caller will see it
                    if(ENDOBJ_STRING.equals(badString) || ENDSTREAM_STRING.equals(badString))
                    {
                        source.rewind(badString.getBytes(ISO_8859_1).length);
                    }
                }
            }
        }
        return retval;
    }

    protected static boolean isDigit(int c)
    {
        return c >= ASCII_ZERO && c <= ASCII_NINE;
    }

    protected void readExpectedChar(char ec) throws IOException
    {
        char c = (char) source.read();
        if (c != ec)
        {
            throw new IOException("expected='" + ec + "' actual='" + c + "' at offset " + source.getPosition());
        }
    }

    private void test() throws IOException{
        byte[] buf = new byte[20];
        source.read(buf);
        StringBuilder builder = new StringBuilder();
        for (byte ss:buf) {
//          Log.w("ceshi","ss:"+ss);
            builder.append((char)ss);
//          Log.w("ceshi","ascii:"+String.valueOf((char)ss));
        }
        Log.w("ceshi","test:"+builder.toString());
    }

    private String parseCOSName() throws IOException {
        source.read();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c = source.read();
        while (c != -1)
        {
            int ch = c;
            if (isEndOfName(ch))
            {
                break;
            }
            else
            {
                buffer.write(ch);
                c = source.read();
            }
        }
        if (c != -1)
        {
            source.rewind(1);
        }

        byte[] bytes = buffer.toByteArray();
        String string;
        if (isValidUTF8(bytes))
        {
            string = new String(buffer.toByteArray(), UTF_8);
        }
        else
        {
            // some malformed PDFs don't use UTF-8 see PDFBOX-3347
            string = new String(buffer.toByteArray(), WINDOWS_1252);
        }
        return string;
    }

    private boolean isValidUTF8(byte[] input)
    {
        CharsetDecoder cs = UTF_8.newDecoder();
        try
        {
            cs.decode(ByteBuffer.wrap(input));
            return true;
        }
        catch (CharacterCodingException e)
        {
            return false;
        }
    }


    private void readXref() throws IOException {
        if (readString().equals("xref")) {
            skipSpaces();
            while (true) {
                String currentLine = readLine();
                String[] splitString = currentLine.split("\\s");
                if (splitString.length != 2)
                {
                    throw new IOException("Unexpected XRefTable Entry: "+currentLine);
                }
                long currObjID;
                try {
                    currObjID = Long.parseLong(splitString[0]);
                } catch (NumberFormatException exception) {
                    throw new IOException("XRefTable: invalid ID for the first object: " + currentLine);
                }
                int count = 0;
                try {
                    count = Integer.parseInt(splitString[1]);
                } catch (NumberFormatException exception) {
                    throw new IOException("XRefTable: invalid number of objects: " + currentLine);
                }
                skipSpaces();
                for(int i = 0; i < count; i++)
                {
                    if(source.isEOF() || isEndOfName((char)source.peek()))
                    {
                        break;
                    }
                    if(source.peek() == 't')
                    {
                        break;
                    }
                    //Ignore table contents
                    currentLine = readLine();
                    splitString = currentLine.split("\\s");
                    if (splitString.length < 3)
                    {
                        Log.w("PdfBox-Android", "invalid xref line: " + currentLine);
                        break;
                    }
                    /* This supports the corrupt table as reported in
                     * PDFBOX-474 (XXXX XXX XX n) */
                    if(splitString[splitString.length-1].equals("n"))
                    {
                        try
                        {
                            long currOffset = Long.parseLong(splitString[0]);
                            int currGenID = Integer.parseInt(splitString[1]);
                            COSObjectKey objKey = new COSObjectKey(currObjID, currGenID);
//                            Log.w("ceshi","currObjID:"+currObjID+","+currGenID);
                            if (!xrefMap.containsKey(objKey))
                                xrefMap.put(objKey,COSNumber.get(currOffset+""));
//                                xrefTrailerResolver.setXRef(objKey, currOffset);
                        }
                        catch (NumberFormatException e)
                        {
                            throw new IOException(e);
                        }
                    }
                    else if(!splitString[2].equals("f"))
                    {
                        throw new IOException("Corrupt XRefTable Entry - ObjID:" + currObjID);
                    }
                    currObjID++;
                    skipSpaces();
                }
                skipSpaces();
                if (!isDigit(source.peek()))
                {
                    break;
                }
            }
        } else {
            throw new IOException("交叉引用流未实现");
        }
    }

    protected String readLine() throws IOException
    {
        if (source.isEOF())
        {
            throw new IOException( "Error: End-of-File, expected line");
        }

        StringBuilder buffer = new StringBuilder( 11 );

        int c;
        while ((c = source.read()) != -1)
        {
            // CR and LF are valid EOLs
            if (isEOL(c))
            {
                break;
            }
            buffer.append( (char)c );
        }
        // CR+LF is also a valid EOL
        if (isCR(c) && isLF(source.peek()))
        {
            source.read();
        }
        return buffer.toString();
    }

    protected boolean isWhitespace( int c )
    {
        return c == 0 || c == 9 || c == 12  || c == ASCII_LF
                || c == ASCII_CR || c == ASCII_SPACE;
    }

    protected boolean isEndOfName(int ch)
    {
        return ch == ASCII_SPACE || ch == ASCII_CR || ch == ASCII_LF || ch == 9 || ch == '>' ||
                ch == '<' || ch == '[' || ch =='/' || ch ==']' || ch ==')' || ch =='(' ||
                ch == 0 || ch == '\f';
    }

    protected void skipSpaces() throws IOException
    {
        int c = source.read();
        // 37 is the % character, a comment
        while( isWhitespace(c) || c == 37)
        {
            if ( c == 37 )
            {
                // skip past the comment section
                c = source.read();
                while(!isEOL(c) && c != -1)
                {
                    c = source.read();
                }
            }
            else
            {
                c = source.read();
            }
        }
        if (c != -1)
        {
            source.rewind(1);
        }
    }

    protected String readString() throws IOException
    {
        skipSpaces();
        StringBuilder buffer = new StringBuilder();
        int c = source.read();
        while( !isEndOfName((char)c) && c != -1 )
        {
            buffer.append( (char)c );
            c = source.read();
        }
        if (c != -1)
        {
            source.rewind(1);
        }
        return buffer.toString();
    }

    protected final long getStartxrefOffset() throws IOException {
        byte[] buf;
        long skipBytes;
        // read trailing bytes into buffer
        Log.w("ceshi","fileLen:"+fileLen);
        try
        {
            final int trailByteCount = (fileLen < readTrailBytes) ? (int) fileLen : readTrailBytes;
            buf = new byte[trailByteCount];
            skipBytes = fileLen - trailByteCount;
            source.seek(skipBytes);
            int off = 0;
            int readBytes;
            while (off < trailByteCount)
            {
                readBytes = source.read(buf, off, trailByteCount - off);
                // in order to not get stuck in a loop we check readBytes (this should never happen)
                if (readBytes < 1)
                {
                    throw new IOException(
                            "No more bytes to read for trailing buffer, but expected: "
                                    + (trailByteCount - off));
                }
                off += readBytes;
            }
        }
        finally
        {
            source.seek(0);
        }
        // find last '%%EOF'
        int bufOff = lastIndexOf(EOF_MARKER, buf, buf.length);
        if (bufOff < 0)
        {
//            if (isLenient)
//            {
                // in lenient mode the '%%EOF' isn't needed
                bufOff = buf.length;
                Log.d("PdfBox-Android", "Missing end of file marker '" + new String(EOF_MARKER) + "'");
//            }
//            else
//            {
//                throw new IOException("Missing end of file marker '" + new String(EOF_MARKER) + "'");
//            }
        }
        // find last startxref preceding EOF marker
        bufOff = lastIndexOf(STARTXREF, buf, bufOff);
        Log.w("ceshi","skipBytes:"+skipBytes);
        Log.w("ceshi","bufOff:"+bufOff);
        if (bufOff < 0)
        {
            throw new IOException("Missing 'startxref' marker.");
        }
        else
        {
            return skipBytes + bufOff;
        }
    }

    protected int lastIndexOf(final char[] pattern, final byte[] buf, final int endOff)
    {
        final int lastPatternChOff = pattern.length - 1;

        int bufOff = endOff;
        int patOff = lastPatternChOff;
        char lookupCh = pattern[patOff];

        while (--bufOff >= 0)
        {
            if (buf[bufOff] == lookupCh)
            {
                if (--patOff < 0)
                {
                    // whole pattern matched
                    return bufOff;
                }
                // matched current char, advance to preceding one
                lookupCh = pattern[patOff];
            }
            else if (patOff < lastPatternChOff)
            {
                // no char match but already matched some chars; reset
                patOff = lastPatternChOff;
                lookupCh = pattern[patOff];
            }
        }
        return -1;
    }


    protected boolean isEOL(int c)
    {
        return isLF(c) || isCR(c);
    }

    private boolean isLF(int c)
    {
        return ASCII_LF == c;
    }

    private boolean isCR(int c)
    {
        return ASCII_CR == c;
    }
}
