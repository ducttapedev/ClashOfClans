import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.jtransforms.fft.DoubleFFT_2D;


public class Tester {
	private static final File PROPERTIES_FILE = new File("coordinates2.prop");
	static Properties prop;
	static AdvancedRobot bot;
	
	public static void main(String[] args) throws AWTException, IOException {
		bot = new AdvancedRobot();
		//bot.delay(2500);
		initProperties();
		//printMouseCoordinatesAndColor(200);
		//dragUp();
		//recruitTroops();
		
		BufferedImage elixirIm = ImageIO.read(new File("elixir2.png"));
		RawImage elixir = getRawImage( elixirIm );
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenRectangle = new Rectangle(screenSize);
		//BufferedImage screenshotIm = bot.createScreenCapture(screenRectangle);
		BufferedImage screenshotIm = ImageIO.read(new File("elixir2.png"));
		RawImage screenshot = getRawImage( screenshotIm );
		
		
		foo(screenshotIm, elixirIm);
		
		List<Point> points = getSubimageCoordinates(screenshotIm, elixirIm);
		for(Point p : points) {
			System.out.println(p);
			bot.mouseMove(p);
			bot.delay(1000);
		}
		
		
		
	}
	
	private static RawImage getRawImage(BufferedImage im, int heightDim, int widthDim) {
		//BufferedImage im = ImageIO.read(new File("fullElixirCropped.png"));
		int width = im.getWidth();
		int height = im.getHeight();
		
		double[][] red = new double[heightDim][widthDim];
		double[][] green = new double[heightDim][widthDim];
		double[][] blue = new double[heightDim][widthDim];
		
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				Color c = new Color(im.getRGB(x, y));
				red[y][x] = c.getRed()/256.0;
				green[y][x] = c.getGreen()/256.0;
				blue[y][x] = c.getBlue()/256.0;
			}
		}
		
		return new RawImage(red, green, blue);
	}
	

	
	
	private static void foo(BufferedImage image, BufferedImage subImage) {
		int height1 = image.getHeight();
		int width1 = image.getWidth();
		//int height2 = subImage.getHeight();
		//int width2 = subImage.getWidth();
		
		RawImage im1 = getRawImage(image, height1, 2*width1);
		RawImage im2 = getRawImage(subImage, height1, 2*width1);
		
		
		// compute FFT of f and g
		DoubleFFT_2D fft1 = new DoubleFFT_2D(height1, width1);
		fft1.realForwardFull(im1.red);
		DoubleFFT_2D fft2 = new DoubleFFT_2D(height1, width1);
		fft2.realForwardFull(im2.red);
		
		// compute F* x G
		double[][] redCC = new double[height1][2*width1];
		
		for(int y = 0; y < height1; y++) {
			for(int x = 0; x < width1; x++) {
				double real1 = im1.red[y][2*x];
				double real2 = im2.red[y][2*x];
				
				// imag1 is negated since we want F* not F
				double imag1 = -im1.red[y][2*x + 1];
				double imag2 = im2.red[y][2*x + 1];
				
				redCC[y][2*x] = real1*real2 - imag1*imag2;
				redCC[y][2*x+1] = real1*imag2 + imag1*real2;
			}
		}
		//redCC[0][0] = im1.red[0][0]*im2.red[0][0];
		//redCC[0][1] = im1.red[0][1]*im2.red[0][1];
		redCC[0][0] = 0;
		//redCC[0][1] = 0;
		
		DoubleFFT_2D fft = new DoubleFFT_2D(height1, width1);
		fft.complexInverse(redCC, false);
		
		System.out.println(toString(redCC));
		System.out.println(redCC.length + "," + redCC[0].length);
		
	}
	
	private static String toString(double[][] arr) {
		String result = "";
		
		for(int y = 0; y < arr.length; y++) {
			for(int x = 0; x < arr[0].length; x++) {
				result += String.format("%8.2f ", arr[y][x]/1.0);
			}
			result += "\n";
		}
		return result;
	}
	

	/**
	 * @param image
	 * @param subImage
	 * @return locations of all top left corners of <code>subImage</code> located in <code>image</code>
	 */
	private static List<Point> getSubimageCoordinates(BufferedImage image, BufferedImage subImage) {
		List<Point> coordinates = new ArrayList<Point>();
		
		for(int x = 0; x < image.getWidth(); x++) {
			for(int y = 0; y < image.getHeight(); y++) {
				// potential top-left corner of subImage
				if(image.getRGB(x, y) == subImage.getRGB(0, 0)) {
					
					// make sure subImage can fit here
					if(x + subImage.getWidth() >= image.getWidth() || y + subImage.getHeight() >= image.getHeight())
						continue;
					
					// see if this part of the image matches the subImage
					for(int xOffset = 0; xOffset < subImage.getWidth(); xOffset++) {
						for(int yOffset = 0; yOffset < subImage.getHeight(); yOffset++) {
							if(image.getRGB(x+xOffset, y+yOffset) != subImage.getRGB(xOffset, yOffset))
								continue;
						}
					}
					coordinates.add(new Point(x, y));
					
				}// if(image.getRGB...)
			}
		}
		return coordinates;
	}

	private static void dragUp() {
		Point top = getPoint("dragUp");
		Point bottom = getPoint("dragDown");
		bot.clickAndDrag(top, bottom);
	}
	
	private static void dragDown() {
		Point top = getPoint("dragUp");
		Point bottom = getPoint("dragDown");
		bot.clickAndDrag(bottom, top);
	}

	private static void recruitTroops() {
		Point barracks = getPoint("barracks");
		Point train = getPoint("train");
		Point archer = getPoint("archer");
		Point barb = getPoint("barb");
		
		
		bot.moveAndClick(barracks);
		bot.moveAndClick(train);
		bot.moveAndClick(archer);
		bot.moveAndClick(barb);
	}


	/**
	 * @param name
	 * @return a <code>Point<code> object containing the coordinates of <code>name</code> as defined by the properties file
	 */
	private static Point getPoint(String name) {
		String[] coords = prop.getProperty(name).split(",");
		int x = Integer.parseInt(coords[0]);
		int y = Integer.parseInt(coords[1]);
		return new Point(x, y);
	}
	
	/**
	 * Print out the mouse coordinates every <code>delay<code> ms
	 * @param delay
	 */
	private static void printMouseCoordinates(int delay) {
		while(true) {
			bot.delay(delay);
			Point location = MouseInfo.getPointerInfo().getLocation();
			System.out.println(location.x + "," + location.y);
		}
	}
	
	/**
	 * Print out the mouse coordinates every <code>delay<code> ms
	 * @param delay
	 */
	private static void printMouseCoordinatesAndColor(int delay) {
		while(true) {
			bot.delay(delay);
			Point location = MouseInfo.getPointerInfo().getLocation();
			System.out.format("%-15s", location.x + "," + location.y);
			System.out.println(bot.getPixelColor(location.x, location.y));
		}
	}

	/**
	 * load the properties file
	 */
	private static void initProperties() {
		// load properties file
		prop = new Properties();
		try {
			FileInputStream inStream = new FileInputStream(PROPERTIES_FILE);
			prop.load(inStream);
			inStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Could not load properties file: " + PROPERTIES_FILE);
		}
	}
	
	private static class RawImage {
		
		public double[][] red, green, blue;
		public final int width, height;
		
		public RawImage(double[][] red, double[][] green, double[][] blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
			width = red.length;
			height = red[0].length;
		}
		
	}
	
	private static RawImage getRawImage(BufferedImage im) {
		//BufferedImage im = ImageIO.read(new File("fullElixirCropped.png"));
		int width = im.getWidth();
		int height = im.getHeight();
		
		double[][] red = new double[width][height];
		double[][] green = new double[width][height];
		double[][] blue = new double[width][height];
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				Color c = new Color(im.getRGB(x, y));
				red[x][y] = c.getRed()/256.0;
				green[x][y] = c.getGreen()/256.0;
				blue[x][y] = c.getBlue()/256.0;
				
			}
		}
		
		return new RawImage(red, green, blue);
	}
	

	
	
	
	 /**
     * Takes a 2D array of grey-levels and a kernel and applies the convolution
     * over the area of the image specified by width and height.
     *
     * @param input the 2D double array representing the image
     * @param width the width of the image
     * @param height the height of the image
     * @param kernel the 2D array representing the kernel
     * @param kernelWidth the width of the kernel
     * @param kernelHeight the height of the kernel
     * @return the 2D array representing the new image
     */
	public static double[][] convolution2D(double[][] input, double[][] kernel) {
		int width = input.length;
		int height = input[0].length;
		
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
		
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        double[][] output = new double[smallWidth][smallHeight];
        for (int i = 0; i < smallWidth; ++i) {
            for (int j = 0; j < smallHeight; ++j) {
                output[i][j] = 0;
            }
        }
        for (int i = 0; i < smallWidth; ++i) {
            for (int j = 0; j < smallHeight; ++j) {
                output[i][j] = singlePixelConvolution(input, i, j, kernel,
                        kernelWidth, kernelHeight);
            }
        }
        return output;
    }
	
	/**
     * Takes an image (grey-levels) and a kernel and a position,
     * applies the convolution at that position and returns the
     * new pixel value.
     *
     * @param input The 2D double array representing the image.
     * @param x The x coordinate for the position of the convolution.
     * @param y The y coordinate for the position of the convolution.
     * @param k The 2D array representing the kernel.
     * @param kernelWidth The width of the kernel.
     * @param kernelHeight The height of the kernel.
     * @return The new pixel value after the convolution.
     */
    public static double singlePixelConvolution(double[][] input,
            int x, int y,
            double[][] k,
            int kernelWidth,
            int kernelHeight) {
        double output = 0;
        for (int i = 0; i < kernelWidth; ++i) {
            for (int j = 0; j < kernelHeight; ++j) {
                output = output + (input[x + i][y + j] * k[kernelWidth-1-i][kernelHeight-1-j]);
            }
        }
        return output;
    }
}

