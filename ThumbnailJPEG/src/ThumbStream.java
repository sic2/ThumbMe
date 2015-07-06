import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

/**
 * ThumbStream allows thumbnail extraction from JPEG.
 * To use it, define the jpeg file when constructing the ThumbStream.
 * Then read from the stream.
 * Finally, remember to close the stream to avoid leaks.
 *
 * Stuff todo: return info about thumbnail.
 */
public class ThumbStream extends InputStream {

    // Standard markers
    // @see http://www.media.mit.edu/pia/Research/deepview/exif.html
    private final int[] JPEG_SOI = {0xff, 0xd8}; // start of image
    private final int[] JPEG_EOI = {0xff, 0xd9}; // end of image
    private final int[] EXIF_HEADER = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00}; // Exif<blank><blank>
    private final int[] MOTOROLA_ALIGN = {0x4d, 0x4d}; // Big-Endian
    private final int[] INTEL_ALIGN = {0x49, 0x49}; // Little-Endian
    private final int[] TIFF_HEADER_TAIL = {0x00, 0x00, 0x00, 0x08};
    // Thumb-markers
    private final int[] THUMB_WIDTH = {0x01, 0x00};
    private final int[] THUMB_HEIGHT = {0x01, 0x01};
    private final int[] COMPRESSION_TYPE = {0x01, 0x03};
    private final int[] JPEG_OFFSET = {0x02, 0x01};
    private final int[] JPEG_SIZE = {0x02, 0x02};
    private final int[] BIT_PER_SAMPLE = {0x01, 0x02};

    // other constants
    private final int IFD_ENTRY_SIZE = 12;
    private final int BYTE_SIZE = 8;
    private final int JPEG_COMPRESSION = 6;
    private final int TIFF_COMPRESSION = 1;

    // Stream/data related fields
    private FileInputStream in;
    private ArrayDeque<Integer> bufferQueue;
    private int pointer;
    private boolean isBigEndian;

    // Thumbnail info
    private boolean hasThumbnail;
    private ThumbInfo info;

    public ThumbStream(String file) throws IOException {
        in = new FileInputStream(file);
        bufferQueue = new ArrayDeque<>();

        // Look at example results from exiftool
        pointer = -IFD_ENTRY_SIZE;
        isBigEndian = false;

        hasThumbnail = false;

        info = new ThumbInfo();
        processThumbnail();
    }

    /**
     * Returns wheather or not the JPEG has an embedded thumbnail
     * @return
     */
    public boolean hasThumbnail() {
        return hasThumbnail;
    }

    /**
     * Return all info about the thumbnail
     * @return
     */
    public ThumbInfo getInfo() {
        return info;
    }

    @Override
    public int read() throws IOException {
        if ((info.getCompressionType() == JPEG_COMPRESSION || info.getCompressionType() == TIFF_COMPRESSION) && !bufferQueue.isEmpty()) {
            return bufferQueue.pop();
        } else {
            return -1;
        }

    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Get the thumbnail data
     * @return
     */
    public int[][] thumbData() {
        return null;
    }

    /////////////////////
    // PRIVATE METHODS //
    /////////////////////

    private void processThumbnail() throws IOException {
        // TODO - check that it is JPEG and has EXIF

        isBigEndian = checkEndianess();

        int entries = IFD0Entries();
        int ifd1loc = IFD1Location(entries);
        hasThumbnail = ifd1loc > 0;
        int ifd1entries = IFD1Entries(ifd1loc);
        updateIFD1Entries(ifd1entries);

        // Get thumbnail data
        fillBufferQueue();
    }

    /**
     * Check whether the JPEG format is in big or little endian.
     * @return
     * @throws IOException
     */
    private boolean checkEndianess() throws IOException {
        findBytes(EXIF_HEADER, true);

        int b_0 = readFile();
        int b_1 = readFile();

        boolean endianess = false;
        if (b_0 == MOTOROLA_ALIGN[0] && b_1 == MOTOROLA_ALIGN[1])
            endianess = true;
        else if (b_0 == INTEL_ALIGN[0] && b_1 == INTEL_ALIGN[1])
            endianess = false;
        else
            throw new IOException();

        return endianess;
    }

    /**
     * Find TIFF Header and then check IFD0 Entries
     */
    private int IFD0Entries() throws IOException {
        int tiff_tail = findTIFF_HEADER_TAIL();
        if (tiff_tail == -1) return -1;

        int b_0 = readFile();
        int b_1 = readFile();
        return eval(new int[] {b_0, b_1}, isBigEndian);
    }

    /**
     * Return the address of the IFD1 entries.
     * @param IFD1Location
     * @param isBigEndian
     * @return
     * @throws IOException
     */
    private int IFD1Entries(int IFD1Location) throws IOException {
        while (pointer < IFD1Location)
            readFile();

        int b_0 = readFile();
        int b_1 = readFile();
        return eval(new int[] {b_0, b_1}, isBigEndian);
    }

    private void updateIFD1Entries(int numberEntries) throws IOException {

        for(int i=0; i < numberEntries; i++) {
            int[] entry = new int[IFD_ENTRY_SIZE];
            readFile(entry);

            // Test width and height

            // Test compression
            if (testBytesTuple(entry, COMPRESSION_TYPE)) {
                info.setCompressionType(entry[9]); // TODO - do not hardcode this
            }
        }
    }

    // Fill the buffer queue with the thumbnail data
    // TODO - test for thumbnail data that is not in JPEG format
    private void fillBufferQueue() throws IOException {
        findBytes(JPEG_SOI, true);

        bufferQueue.add(JPEG_SOI[0]);
        bufferQueue.add(JPEG_SOI[1]);

        int index = 0;
        while(true) {

            int b = readFile();
            if (b == -1) break;

            if (b == JPEG_EOI[index]) {
                index++;
            } else {
                index = 0;
            }

            bufferQueue.add(b);
            if (index == JPEG_EOI.length)
                break;
        }
    }

    private int findTIFF_HEADER_TAIL() throws IOException {
        return findBytes(TIFF_HEADER_TAIL);

    }

    /**
     * Return the pointer position in the JPEG file where the int[] bytes occur + bytes.length
     * @param bytes
     * @param isBigEndian
     * @return
     * @throws IOException
     */
    private int findBytes(int[] bytes, boolean isBigEndian) throws IOException {
        int index = 0;

        while (true) {
            int b = readFile();
            if (b == -1) return -1;

            if (isBigEndian && b == bytes[index] || !isBigEndian && b == bytes[bytes.length - index - 1]) {
                index++;
            } else {
                index = 0;
            }

            if (index == bytes.length)
                return pointer;
        }
    }

    /**
     * Return the pointer position in the JPEG file where the int[] bytes occur + bytes.length
     * Use JPEG endianness
     * @param bytes
     * @return
     * @throws IOException
     */
    private int findBytes(int[] bytes) throws IOException {
        return findBytes(bytes, isBigEndian);
    }

    /**
     * Check if b_0 and b_1 are the first bytes in bytes
     * @param bytes
     * @param test
     * @return
     */
    private boolean testBytesTuple(int[] bytes, int[] test) {
        if (bytes.length < 2) return false;

        return (bytes[0] == test[0] && bytes[1] == test[1] && isBigEndian) || (bytes[1] == test[0] && bytes[0] == test[1] && !isBigEndian);
    }


    /**
     * Find the IFD1 Location address in the JPEG file given the number of IFD0 entries.
     * This method must be called just after the number of IFD0 entries is found.
     * @param IFD0entries
     * @return
     * @throws IOException
     */
    private int IFD1Location(int IFD0entries) throws IOException {
        int ifd0offset = IFD0entries * (IFD_ENTRY_SIZE);

        for(int i = 0; i < ifd0offset; i++) {
             readFile();
        }

        int b_0 = readFile();
        int b_1 = readFile();
        int b_2 = readFile();
        int b_3 = readFile();

        return eval(new int[]{b_0, b_1, b_2, b_3}, isBigEndian);
    }

    /**
     * Returns a byte of the embedded file and increases the pointer.
     * @return
     * @throws IOException
     */
    private int readFile() throws IOException {
        int b = in.read();
        pointer++;
        return b;
    }

    /**
     * Read the file to an output array.
     * @param out
     * @throws IOException
     */
    private void readFile(int[] out) throws IOException {
        for(int i = 0; i < out.length; i++)
            out[i] = readFile();
    }

    /**
     * Evaluate n-bytes stored in value as an integer.
     * Returns -1 if integer in value overflows the int space.
     * @param value
     * @param isBigEndian
     * @return
     */
    private int eval(int[] value, boolean isBigEndian) {
        if (value.length > 4)
            return -1;

        int retval = 0;
        if (isBigEndian) {
            for(int i = value.length - 1, j = 0; i >=0; i--, j++) {
                retval |= value[i] << j*BYTE_SIZE;
            }
        }  else {
            for(int i = 0; i < value.length; i++) {
                retval |= value[i] << i*BYTE_SIZE;
            }
        }

        return retval;
    }
}
