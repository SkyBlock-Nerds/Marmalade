package net.hypixel.nerdbot.marmalade.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the GIF encoding contract: GIF only supports 1-bit alpha, so semi-transparent
 * pixels must survive as opaque pixels (not vanish), fully transparent pixels must stay
 * transparent, and each frame must dispose to the background so animations with
 * transparency do not stack frames on top of each other.
 */
class ImageUtilGifTest {

    private static final int OPAQUE_RED = 0xFFFF0000;
    private static final int SEMI_TRANSPARENT_BLUE = (200 << 24) | 0x0000FF;
    private static final int TRANSPARENT = 0x00000000;

    @Test
    void semiTransparentPixelsSurviveGifEncoding() throws IOException {
        BufferedImage frame = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        frame.setRGB(0, 0, OPAQUE_RED);
        frame.setRGB(1, 0, SEMI_TRANSPARENT_BLUE);
        frame.setRGB(2, 0, TRANSPARENT);

        byte[] gif = ImageUtil.toGifBytes(List.of(frame), 50, true);
        BufferedImage decoded = decodeFrame(gif, 0);

        assertEquals(OPAQUE_RED, decoded.getRGB(0, 0), "opaque pixel keeps its color");
        assertEquals(0xFF0000FF, decoded.getRGB(1, 0), "semi-transparent pixel survives as opaque");
        assertEquals(0, decoded.getRGB(2, 0) >>> 24, "fully transparent pixel stays transparent");
    }

    @Test
    void framesDisposeToBackground() throws IOException {
        BufferedImage frameA = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        frameA.setRGB(0, 0, OPAQUE_RED);
        BufferedImage frameB = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        frameB.setRGB(3, 3, OPAQUE_RED);

        byte[] gif = ImageUtil.toGifBytes(List.of(frameA, frameB), 50, true);

        for (int i = 0; i < 2; i++) {
            assertEquals("restoreToBackgroundColor", readDisposalMethod(gif, i),
                "frame " + i + " must clear before the next frame draws");
        }
    }

    @Test
    void inputFramesAreNotMutated() throws IOException {
        BufferedImage frame = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        frame.setRGB(0, 0, SEMI_TRANSPARENT_BLUE);

        ImageUtil.toGifBytes(List.of(frame), 50, true);

        assertEquals(SEMI_TRANSPARENT_BLUE, frame.getRGB(0, 0),
            "caller frames must keep their original alpha (they are reused for WebP encoding)");
    }

    @Test
    void opaqueFramesRoundTripUnchanged() throws IOException {
        // Checkerboard rather than lone pixels: the JDK palette quantizer folds colors with
        // very few pixels into neighboring palette entries, which is not what this test pins.
        BufferedImage frame = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                frame.setRGB(x, y, (x + y) % 2 == 0 ? 0xFF0000 : 0x00FF00);
            }
        }

        byte[] gif = ImageUtil.toGifBytes(List.of(frame), 50, true);
        BufferedImage decoded = decodeFrame(gif, 0);

        assertEquals(0xFFFF0000, decoded.getRGB(0, 0), "opaque RGB frame keeps red");
        assertEquals(0xFF00FF00, decoded.getRGB(1, 0), "opaque RGB frame keeps green");
    }

    private static BufferedImage decodeFrame(byte[] gif, int index) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(gif))) {
            ImageReader reader = firstGifReader(in);
            BufferedImage image = reader.read(index);
            reader.dispose();
            return image;
        }
    }

    private static String readDisposalMethod(byte[] gif, int index) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(gif))) {
            ImageReader reader = firstGifReader(in);
            IIOMetadata metadata = reader.getImageMetadata(index);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            reader.dispose();
            return gce.getAttribute("disposalMethod");
        }
    }

    private static ImageReader firstGifReader(ImageInputStream in) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
        assertTrue(readers.hasNext(), "JDK GIF reader available");
        ImageReader reader = readers.next();
        reader.setInput(in);
        return reader;
    }
}
