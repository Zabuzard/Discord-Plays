package io.github.zabuzard.discordplays.discord.gif

import java.awt.image.RenderedImage
import javax.imageio.IIOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageOutputStream

//
//  GifSequenceWriter.java
//
//  Created by Elliot Kroo on 2009-04-25.
//
// This work is licensed under the Creative Commons Attribution 3.0 Unported
// License. To view a copy of this license, visit
// http://creativecommons.org/licenses/by/3.0/ or send a letter to Creative
// Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
@OptIn(ExperimentalUnsignedTypes::class)
internal class GifSequenceWriter(
    outputStream: ImageOutputStream,
    imageType: Int,
    timeBetweenFramesMS: Int,
    shouldLoop: Boolean = false
) {
    private val gifWriter: ImageWriter = getWriter()
    private val imageWriteParam: ImageWriteParam = gifWriter.defaultWriteParam
    private val imageMetaData: IIOMetadata

    /**
     * Creates a new GifSequenceWriter
     *
     * @param outputStream the ImageOutputStream to be written to
     * @param imageType one of the imageTypes specified in BufferedImage
     * @param timeBetweenFramesMS the time between frames in miliseconds
     * @param loopContinuously wether the gif should loop repeatedly
     * @throws IIOException if no gif ImageWriters are found
     * @author Elliot Kroo (elliot[at]kroo[dot]net)
     */
    init {
        // my method to create a writer
        val imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType)
        imageMetaData = gifWriter.getDefaultImageMetadata(
            imageTypeSpecifier,
            imageWriteParam
        )
        val metaFormatName = imageMetaData.nativeMetadataFormatName
        val root = imageMetaData.getAsTree(metaFormatName) as IIOMetadataNode
        val graphicsControlExtensionNode = getNode(
            root,
            "GraphicControlExtension"
        )
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none")
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute(
            "transparentColorFlag",
            "FALSE"
        )
        graphicsControlExtensionNode.setAttribute(
            "delayTime",
            (timeBetweenFramesMS / 10).toString()
        )
        graphicsControlExtensionNode.setAttribute(
            "transparentColorIndex",
            "0"
        )
        val commentsNode = getNode(root, "CommentExtensions")
        commentsNode.setAttribute("CommentExtension", "Created by MAH")

        if (shouldLoop) {
            val appExtensionsNode = getNode(
                root,
                "ApplicationExtensions"
            )
            val child = IIOMetadataNode("ApplicationExtension")
            child.setAttribute("applicationID", "NETSCAPE")
            child.setAttribute("authenticationCode", "2.0")

            child.userObject = ubyteArrayOf(0x1u, 0x0u, 0x0u).toByteArray()
            appExtensionsNode.appendChild(child)
        }

        imageMetaData.setFromTree(metaFormatName, root)
        gifWriter.output = outputStream
        gifWriter.prepareWriteSequence(null)
    }

    fun writeToSequence(img: RenderedImage) {
        gifWriter.writeToSequence(IIOImage(img, null, imageMetaData), imageWriteParam)
    }

    /**
     * Close this GifSequenceWriter object. This does not close the underlying
     * stream, just finishes off the GIF.
     */
    fun close() {
        gifWriter.endWriteSequence()
    }

    companion object {
        private fun getWriter(): ImageWriter {
            val iter = ImageIO.getImageWritersBySuffix("gif")
            return if (!iter.hasNext()) {
                throw IIOException("No GIF Image Writers Exist")
            } else {
                iter.next()
            }
        }

        /**
         * Returns an existing child node, or creates and returns a new child node
         * (if the requested node does not exist).
         *
         * @param rootNode the `IIOMetadataNode` to search for the child node.
         * @param nodeName the name of the child node.
         * @return the child node, if found or a new node created with the given
         *     name.
         */
        private fun getNode(
            rootNode: IIOMetadataNode,
            nodeName: String
        ): IIOMetadataNode {
            val nNodes = rootNode.length
            for (i in 0 until nNodes) {
                if (rootNode.item(i).nodeName.compareTo(nodeName, ignoreCase = true) == 0) {
                    return rootNode.item(i) as IIOMetadataNode
                }
            }
            val node = IIOMetadataNode(nodeName)
            rootNode.appendChild(node)
            return node
        }
    }
}
