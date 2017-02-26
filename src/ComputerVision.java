/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.imageio.ImageIO;

import boofcv.alg.feature.detect.template.TemplateMatching;
import boofcv.alg.feature.detect.template.TemplateMatchingIntensity;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.template.FactoryTemplateMatching;
import boofcv.factory.feature.detect.template.TemplateScoreType;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.feature.Match;
import boofcv.struct.image.ImageFloat32;
import imageutil.GaussianFilter;

/**
 * Example of how to find objects inside an image using template matching.  Template matching works
 * well when there is little noise in the image and the object's appearance is known and static.
 *
 * @author Peter Abeles
 */
public class ComputerVision {

	public static RenderedImage getRenderedImage(BufferedImage image) {
		Graphics2D g2 = image.createGraphics();
		g2.drawImage(image, null, null);
		return (RenderedImage)image;
	}
	
	public static ImageFloat32 getImageFloat32(BufferedImage screenshotIm) {
		ImageFloat32 image = new ImageFloat32(screenshotIm.getWidth(), screenshotIm.getHeight());
		ConvertBufferedImage.convertFrom(screenshotIm, image);
		return image;
	}

	/**
	 * Demonstrates how to search for matches of a template inside an image
	 *
	 * @param image           Image being searched
	 * @param template        Template being looked for
	 * @param expectedMatches Number of expected matches it hopes to find
	 * @return List of match location and scores
	 */
	public static List<Match> findMatches(ImageFloat32 image, ImageFloat32 template,
										   int expectedMatches) {
		
		// create template matcher.
		TemplateMatching<ImageFloat32> matcher =
				FactoryTemplateMatching.createMatcher(TemplateScoreType.SUM_DIFF_SQ, ImageFloat32.class);

		// Find the points which match the template the best
		matcher.setTemplate(template, expectedMatches);
		matcher.process(image);

		return matcher.getResults().toList();

	}

	/**
	 * Computes the template match intensity image and displays the results. Brighter intensity indicates
	 * a better match to the template.
	 */
	public static void showMatchIntensity(ImageFloat32 image, ImageFloat32 template) {

		// create algorithm for computing intensity image
		TemplateMatchingIntensity<ImageFloat32> matchIntensity =
				FactoryTemplateMatching.createIntensity(TemplateScoreType.SUM_DIFF_SQ, ImageFloat32.class);

		// apply the template to the image
		matchIntensity.process(image, template);

		// get the results
		ImageFloat32 intensity = matchIntensity.getIntensity();

		// adjust the intensity image so that white indicates a good match and black a poor match
		// the scale is kept linear to highlight how ambiguous the solution is
		float min = ImageStatistics.min(intensity);
		float max = ImageStatistics.max(intensity);
		float range = max - min;
		PixelMath.plus(intensity, -min, intensity);
		PixelMath.divide(intensity, range, intensity);
		PixelMath.multiply(intensity, 255.0f, intensity);

		BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_BGR);
		VisualizeImageData.grayMagnitude(intensity, output, -1);
		ShowImages.showWindow(output, "Match Intensity");
	}

	/**
	 * @param image
	 * @param template
	 * @return match with the smallest average pixel distance
	 */
	private static Match getClosestMatch(BufferedImage image, BufferedImage template) {
		List<Match> matches = getMatches(image, template, 20);
		Match best = matches.get(0);
		
		for(Match m : matches) {
			if(m.score < best.score)
				best = m;
		}
		return best;
	}
	
	/**
	 * @param image
	 * @param template
	 * @param numMatches
	 * @return a list of coordinates where the <code>template</code> matches a certain subImage of <code>image</code>.
	 * Note that the match score is replaced by our own score, which is equal to the average color distance between the subImage and the template.
	 */
	private static List<Match> getMatches(BufferedImage image, BufferedImage template, int numMatches) {
		int width = template.getWidth();
		int height = template.getHeight();
		
		// convert screenshot and template to ImageFloat32
		ImageFloat32 screen32 = new ImageFloat32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, screen32);
		ImageFloat32 template32 = new ImageFloat32(template.getWidth(), template.getHeight());
		ConvertBufferedImage.convertFrom(template, template32);
		
		List<Match> matches = ComputerVision.findMatches(screen32, template32, numMatches);
		
		// check the goodness of each match
		for(int i = 0; i < matches.size(); i++) {
			Match match = matches.get(i);
			Rectangle r = new Rectangle(match.x, match.y, width, height);
			//TODO: how do we handle overlapping matches?
			/*
			for(int j = 0; j < i; j++) {
				Rectangle r0 = new Rectangle(matches.get(j).x, matches.get(j).y, width, height);
				//if(r.intersects(r0))
			}
			*/
			
			
			// get the average pixel color distance
			match.score = ComputerVision.getAverageDiff(image, template, match);
			
		}
		
		return matches;
				
	}
	
	/**
	 * @param screen
	 * @param template
	 * @param match
	 * @return average difference across all channels of all pixels
	 */
	public static double getAverageDiff(BufferedImage screen, BufferedImage template, Match match) {
		int width = template.getWidth();
		int height = template.getHeight();
		double averageDiff = 0;
		double maxDiff = 0;
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				Color templateColor = new Color(template.getRGB(x, y));
				Color screenColor = new Color(screen.getRGB(match.x + x, match.y + y));
				
				int diff = Math.abs(templateColor.getRed() - screenColor.getRed())
						+ Math.abs(templateColor.getGreen() - screenColor.getGreen())
						+ Math.abs(templateColor.getBlue() - screenColor.getBlue());
				if(diff > maxDiff)
					maxDiff = diff;
				averageDiff += diff/3.0;
			}
		}
		
		averageDiff /= (width*height);
		maxDiff /= 3.0;
		return averageDiff;
	}

	/**
	 * @param image
	 * @param stdDev standard deviation of the gaussian; 0 if no blur
	 * @return
	 */
	public static BufferedImage blur(BufferedImage image, float stdDev) {
		if(stdDev == 0)
			return image;
		
		
		GaussianFilter f = new GaussianFilter(5, stdDev);
		BufferedImage filteredImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		f.filter(image, filteredImage);
		
		return filteredImage;
	}

	/**
	 * Helper function will is finds matches and displays the results as colored rectangles
	 */
	private static void drawRectangles(Graphics2D g2,
									   BufferedImage image, Template template,
									   int expectedMatches) {
		
		template.refresh(image);
		template.drawRectangles(g2);
		System.out.println(template.toString());
		//List<Match> found = getMatches(image, template, expectedMatches);
	
		/*
		int r = 2;
		int w = template.getWidth() + 2 * r;
		int h = template.getHeight() + 2 * r;
	
		g2.setStroke(new BasicStroke(3));
		for (Match m : found) {
			
			//double averageDiff = getAverageDiff(image, template, m);
			if(m.score < 10) {
				drawBox(g2, r, w, h, m);
			}
		}
		
		System.out.println();
		g2.setColor(Color.BLUE);
		
		for (Match m : found) {
			//double averageDiff = getAverageDiff(image, template, m);
			if(m.score >= 10) {
				drawBox(g2, r, w, h, m);
			}
		}
		*/
	}

	private static void drawBox(Graphics2D g2, int r, int w, int h, Match m) {
		//System.out.println(m.x + "," + m.y + ":\t" + m.score + "\t>>> " +  averageDiff);
		System.out.println(m.x + "," + m.y + ":\t" + m.score);
		
		// the return point is the template's top left corner
		int x0 = m.x - r;
		int y0 = m.y - r;
		int x1 = x0 + w;
		int y1 = y0 + h;

		g2.drawLine(x0, y0, x1, y0);
		g2.drawLine(x1, y0, x1, y1);
		g2.drawLine(x1, y1, x0, y1);
		g2.drawLine(x0, y1, x0, y0);
	}

	public static void main(String args[]) throws AWTException, IOException {
		//Tester.bot = new AdvancedRobot();
		
		
		// Load image and templates
		Robot bot = new Robot();
		bot.delay(2000);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenRectangle = new Rectangle(screenSize);
		
		BufferedImage imageB = bot.createScreenCapture(screenRectangle);
		//BufferedImage imageB = ImageIO.read(new File("base.png"));
		ImageFloat32 image = getImageFloat32(imageB);
		
		//image = UtilImageIO.loadImage("2014-10-24_16-41-26.png", ImageFloat32.class);
		//BufferedImage templateB = ImageIO.read(new File("img\\king.png"));

		Template templateB = Template.createTemplate("half full max collector, img\\queen.png, 1, 0f");
		//ImageFloat32 template = getImageFloat32(templateB);
		
		// create output image to show results
		BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_BGR);
		ConvertBufferedImage.convertTo(image, output);
		Graphics2D g2 = output.createGraphics();
	
		// Searches for a small 'x' that indicates where a window can be closed
		// Only two such objects are in the image so at best there will be one false positive
		g2.setColor(Color.RED);
		drawRectangles(g2, imageB, templateB, 20);
		// show match intensity image for this template
		//showMatchIntensity(image, template);
	
		// False positives can some times be pruned using the error score.  In photographs taken
		// in the real world template matching tends to perform very poorly
	
		ShowImages.showWindow(output, "Found Matches");
	}
}