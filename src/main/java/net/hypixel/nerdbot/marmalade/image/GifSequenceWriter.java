//
//  GifSequenceWriter.java
//
//  Created by Elliot Kroo on 2009-04-25.
//  Original source: https://gist.github.com/jesuino/528703e7b1974d857b36
//
// This work is licensed under the Creative Commons Attribution 3.0 Unported
// License. To view a copy of this license, visit
// http://creativecommons.org/licenses/by/3.0/ or send a letter to Creative
// Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
//
package net.hypixel.nerdbot.marmalade.image;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GifSequenceWriter {
    protected ImageWriter gifWriter;
    protected ImageWriteParam imageWriteParam;
    protected IIOMetadata imageMetaData;
    protected List<BufferedImage> frames;

    public GifSequenceWriter(ImageOutputStream output, int imageType, int timeBetweenFramesMS, boolean loopContinuously) throws IOException {
        gifWriter = getWriter();
        imageWriteParam = gifWriter.getDefaultWriteParam();
        // Frames are normalized to TYPE_INT_ARGB before encoding (see toGifCompatibleFrame),
        // so the metadata must be derived from ARGB as well. Deriving it from other types
        // makes the JDK encoder palettize against the wrong color table and lose colors.
        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

        String metaFormatName = imageMetaData.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);
        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

        // Clear each frame before drawing the next one. With "none", frames with transparent
        // regions accumulate on top of each other (visible as ghosting on animated text).
        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by MAH");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loop = loopContinuously ? 0 : 1;
        child.setUserObject(new byte[]{0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF)});
        appExtensionsNode.appendChild(child);

        imageMetaData.setFromTree(metaFormatName, root);
        gifWriter.setOutput(output);
        gifWriter.prepareWriteSequence(null);

        // Initialize the frames list
        frames = new ArrayList<>();
    }

    private static ImageWriter getWriter() throws IIOException {
        Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
        if (!iter.hasNext()) {
            throw new IIOException("No GIF Image Writers Exist");
        } else {
            return iter.next();
        }
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        BufferedImage frame = toGifCompatibleFrame(img);
        gifWriter.writeToSequence(new IIOImage(frame, null, imageMetaData), imageWriteParam);

        // Add the frame to the frames list
        frames.add(frame);
    }

    /**
     * Prepares a frame for GIF encoding. GIF supports only 1-bit alpha and the JDK encoder
     * maps every pixel with alpha below 255 to the transparent index, so semi-transparent
     * artwork silently disappears. Force any visible pixel to full opacity while keeping
     * fully transparent pixels transparent. Frames are also converted to TYPE_INT_ARGB,
     * which the JDK encoder palettizes reliably; other types can lose colors entirely.
     * The input image is never mutated because callers reuse frames for other encodings.
     *
     * @param img The frame to prepare.
     *
     * @return An ARGB frame with binary alpha, or the input if it already satisfies both.
     */
    private static BufferedImage toGifCompatibleFrame(RenderedImage img) {
        BufferedImage source;
        if (img instanceof BufferedImage buffered && buffered.getType() == BufferedImage.TYPE_INT_ARGB) {
            source = buffered;
        } else {
            source = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = source.createGraphics();
            graphics.drawRenderedImage(img, null);
            graphics.dispose();
        }

        if (!hasPartialAlpha(source)) {
            return source;
        }

        BufferedImage normalized = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    argb |= 0xFF000000;
                }
                normalized.setRGB(x, y, argb);
            }
        }
        return normalized;
    }

    private static boolean hasPartialAlpha(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    return true;
                }
            }
        }
        return false;
    }

    public BufferedImage toBufferedImage() {
        if (frames.isEmpty()) {
            throw new IllegalStateException("No frames in the GIF");
        }

        // Create a BufferedImage to hold the entire animated GIF
        int width = frames.get(0).getWidth();
        int height = frames.get(0).getHeight();
        BufferedImage gifImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gifImage.createGraphics();

        // Draw each frame onto the final image
        for (BufferedImage frame : frames) {
            g.drawImage(frame, 0, 0, null);
        }

        g.dispose();
        return gifImage;
    }

    public void close() throws IOException {
        gifWriter.endWriteSequence();
    }
}
