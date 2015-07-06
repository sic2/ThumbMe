# ThumbMe
A simple and easy-to-use Java class to get the embedded thumbnail image from a JPEG

## How to use it
* Copy the ThumbStream.java into your project
* Then use the stream this way:
``ThumbStream ts = new ThumbStream("test.jpg");``
* The ThumbStream does return the embedded thumbnail in the JPEG image on ``read()`` functions

Some JPEG images do not have any embedded thumbnail. In that case you can check if the thumbnail exists by calling ``ts.hasThumbnail()``

### Example
```
ThumbStream ts = new ThumbStream("test.jpg");
final Path destination = Paths.get("test-thumb.jpg");
Files.copy(ts, destination);
```

## TODO
* Capture thumbnail info (width, height, compression)
* Test against JPEG with thumbnail in TIFF format
