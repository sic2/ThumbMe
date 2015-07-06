/**
 * Info about Thumbnail.
 * WORK IN PROGRESS
 */
public class ThumbInfo {
    private int compressionType = -1; // default error code

    // Dummy values
    private int thumbLength = -1;
    private int thumbOffset = -1;
    private int samplesPerPixel = -1;

    public int getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(int compressionType) {
        this.compressionType = compressionType;
    }

    public int getThumbLength() {
        return thumbLength;
    }

    public void setThumbLength(int thumbLength) {
        this.thumbLength = thumbLength;
    }

    public int getThumbOffset() {
        return thumbOffset;
    }

    public void setThumbOffset(int thumbOffset) {
        this.thumbOffset = thumbOffset;
    }

    public int getSamplesPerPixel() {
        return samplesPerPixel;
    }

    public void setSamplesPerPixel(int samplesPerPixel) {
        this.samplesPerPixel = samplesPerPixel;
    }
}
