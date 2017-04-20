package org.luoyh.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class ImageUtils {

	/**
	 * If throws exception, the file not an image.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getImage(File file) throws IOException {
		return ImageIO.read(file);
	}

	public static BufferedImage getImage(InputStream in) throws IOException {
		return ImageIO.read(in);
	}

	/**
	 * true is Image file.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static boolean isImage(File file) {
		try {
			if (null == getImage(file))
				return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static boolean isImage(InputStream in) {
		try {
			if (null == getImage(in))
				return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * The result index 0 is the image file width, index 1 is the image file height.
	 * 
	 * @param file
	 * @return [width, height]
	 * @throws IOException
	 */
	public static int[] getImagePixel(File file) throws IOException {
		BufferedImage bufferedImage = getImage(file);
		if (null == bufferedImage)
			throw new IllegalArgumentException("Not an image file");
		return new int[] { bufferedImage.getWidth(), bufferedImage.getHeight() };
	}

	public static int[] getImagePixel(InputStream in) throws IOException {
		BufferedImage bufferedImage = getImage(in);
		if (null == bufferedImage)
			throw new IllegalArgumentException("Not an image file");
		return new int[] { bufferedImage.getWidth(), bufferedImage.getHeight() };
	}

	/**
	 * Get image file format name.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 * @throws Throwable
	 */
	public static String getImageFormatName(File file) throws IOException {
		try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
			Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
			if (!iter.hasNext())
				return null;
			ImageReader reader = iter.next();
			return reader.getFormatName();
		} catch (IOException e) {
			throw e;
		}
	}

	public static void main(String[] args) throws Exception {
		File file = new File("f:/timg/e");
		System.out.println(isImage(file));
		ImageInputStream iis = ImageIO.createImageInputStream(file);
		Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
		if (!iter.hasNext()) {
			throw new IllegalArgumentException("Not an Image");
		}
		ImageReader reader = iter.next();
		iis.close();
		System.out.println(reader.getFormatName());
	}
}
