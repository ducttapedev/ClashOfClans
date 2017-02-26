import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.feature.Match;
import boofcv.struct.image.ImageFloat32;


public class Template {
	private static final int NUM_MATCHES = 40;
	
	private final String name;
	private final BufferedImage template;
	private final double maxDiff;
	private final double multiplier;
	private final float blurStdDev;
	
	private List<Match> matches;
	private List<Match> strongMatches;
	//private Match strongestMatch;
	
	public static Template createTemplate(String s) {
		String[] params = s.split(", ");
		Main.LOGGER.info("Loading template: " + s);
		
		double maxDiff = Double.parseDouble(params[2]);
		float stdDev = Float.parseFloat(params[3]);
		
		// multiplier is optional
		double multiplier = 0;
		if(params.length > 4)
			multiplier = Double.parseDouble(params[4]);
		
		//System.out.println(maxDiff + "," + stdDev);
		
		try {
			return new Template(params[0], ImageIO.read(new File(params[1])), maxDiff, multiplier, stdDev);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Detects overlapping matches and removes the lower scoring one.
	 * @param templates
	 */
	public static void removeOverlappingMatches(Collection<Template> templates) {
		for(Template t1: templates) {
			for(Template t2: templates) {
				// get width and height of each template
				int width1 = t1.getImageWidth();
				int height1 = t1.getImageHeight();
				int width2 = t2.getImageWidth();
				int height2 = t2.getImageHeight();
				
				for(int i = 0; i < t1.strongMatches.size(); i++) {
					Match m1 = t1.strongMatches.get(i);
					// compute rectangle of first match
					Rectangle r1 = new Rectangle(m1.x, m1.y, width1, height1);
					for(int j = 0; j < t2.strongMatches.size(); j++) {
						Match m2 = t2.strongMatches.get(j);
						if(t1 == t2 && m1 == m2)
							continue;
						// compute rectangle of second match
						Rectangle r2 = new Rectangle(m2.x, m2.y, width2, height2);
						
						// if first and second match intersect, remove the lower scoring one
						if(r1.intersects(r2)) {
							if(t1.multiplier > t2.multiplier)
								t2.strongMatches.remove(m2);
							else
								t1.strongMatches.remove(m1);
						}
					}
				}
			}
			
		}
	}
	
	private Template(String name, BufferedImage image, double maxScore, double maxDiff, float blurStdDev) {
		this.name = name;
		this.template = ComputerVision.blur(image, blurStdDev);
		this.maxDiff = maxScore;
		this.multiplier = maxDiff;
		this.blurStdDev = blurStdDev;
	}
	
	public String getName() {
		return name;
	}
	
	public int getImageWidth() {
		return template.getWidth();
	}
	
	public int getImageHeight() {
		return template.getHeight();
	}
	
	public boolean isNamed(String s) {
		return name.equals(s);
	}
	
	/**
	 * @return screenshot blurred to this template's <code>blurStdDev</code>
	 */
	public BufferedImage getScreenshot() {
		return ComputerVision.blur(Main.getScreenshot(), blurStdDev);
	}
	
	/**
	 * Refreshes the matches, strongMatches, and numMatches to reflect the new <code>screen</code>
	 * @param screen
	 */
	public void refresh(BufferedImage scr) {
		BufferedImage screen = ComputerVision.blur(scr, blurStdDev);
		matches = getMatches(screen);
		strongMatches = new ArrayList<Match>();
		
		
		// determine which matches are strong, and find the strongest (the one with the minimum score)
		for(Match m : matches) {
			if(m.score <= maxDiff) {
				strongMatches.add(m);
			}
		}
	}
	
	
	/**
	 * @return number of strong matches
	 */
	public int numStrongMatches() {
		return strongMatches.size();
	}
	
	/**
	 * @return list of strong matches
	 */
	private List<Match> getStrongMatches() {
		return strongMatches;
	}
	
	/**
	 * @return the only strong match if there is exactly one, null otherwise
	 */
	public Match getStrongMatch() {
		if(strongMatches.size() == 1)
			return strongMatches.get(0);
		return null;
	}
	
	/**
	 * @return list of matches
	 */
	private List<Match> getMatches() {
		return matches;
	}
	
	public void drawRectangles(Graphics2D g2) {
		g2.setStroke(new BasicStroke(3));
		
		// draw all matches in blue
		g2.setColor(Color.BLUE);
		drawMatches(g2, matches);
		
		// redraw strong matches in red
		g2.setColor(Color.RED);
		drawMatches(g2, strongMatches);
		
	}

	private void drawMatches(Graphics2D g2, List<Match> matches) {
		int r = 2;
		int w = template.getWidth() + 2 * r;
		int h = template.getHeight() + 2 * r;
		
		for(Match m : matches) {
			int x0 = m.x - r;
			int y0 = m.y - r;
			int x1 = x0 + w;
			int y1 = y0 + h;

			g2.drawLine(x0, y0, x1, y0);
			g2.drawLine(x1, y0, x1, y1);
			g2.drawLine(x1, y1, x0, y1);
			g2.drawLine(x0, y1, x0, y0);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(name + "\n");
		
		for(int i = 0; i < strongMatches.size(); i++)
				s.append(strongMatches.get(i).toString() + "\n");
		s.append("-----------------------------------------\n");
		
		return s.toString();
	}

	/**
	 * @param image
	 * @param template
	 * @param numMatches
	 * @return a list of coordinates where the <code>template</code> matches a certain subImage of <code>image</code>.
	 * Note that the match score is replaced by our own score, which is equal to the average color distance between the subImage and the template, averaged over all channels of all pixels.
	 */
	public List<Match> getMatches(BufferedImage image) {
		//int width = template.getWidth();
		//int height = template.getHeight();
		
		// convert screenshot and template to ImageFloat32
		ImageFloat32 screen32 = new ImageFloat32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, screen32);
		ImageFloat32 template32 = new ImageFloat32(template.getWidth(), template.getHeight());
		ConvertBufferedImage.convertFrom(template, template32);
		
		List<Match> matches = ComputerVision.findMatches(screen32, template32, NUM_MATCHES);
		
		// check the goodness of each match
		for(int i = 0; i < matches.size(); i++) {
			Match match = matches.get(i);
			//TODO: how do we handle overlapping matches?
			/*
			Rectangle r = new Rectangle(match.x, match.y, width, height);
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
	 * @return the score for this template. A higher score indicates more resources available
	 */
	public double getScore() {
		return numStrongMatches()*multiplier;
	}
	
	/**
	 * @param image
	 * @param template
	 * @return match with the smallest average pixel distance
	 *
	public Match getClosestMatch(BufferedImage image) {
		List<Match> matches = getMatches(image);
		Match best = matches.get(0);
		
		for(Match m : matches) {
			if(m.score < best.score)
				best = m;
		}
		return best;
	}*/
}
