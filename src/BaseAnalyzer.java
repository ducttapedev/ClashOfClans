import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.imageio.ImageIO;


public class BaseAnalyzer {
	//Template t1, t2;
	private Map<String, Template> templates;
	private BufferedImage screen;//, topScreen, bottomScreen;
	
	/*
	public BaseAnalyzer(Template almostFullCollector, Template halfFullMaxCollector) {
		this.t1 = almostFullCollector;
		this.t2 = halfFullMaxCollector;
	}
	*/
	public BaseAnalyzer(Map<String, Template> templates) {
		this.templates = templates;
	}
	
	/**
	 * @return true if we should attack this base
	 */
	@Deprecated
	public boolean shouldAttack() {
		screen = concat(Main.getTopScreen(), Main.getBottomScreen());
		Template t1 = templates.get("almost full collector");
		Template t2 = templates.get("half full max collector");
		
		// refresh all templates
		//for(Template t : templates.values())
			//t.refresh(screen);
		
		// refresh these templates
		t1.refresh(screen);
		t2.refresh(screen);
				
		// check if base has at least 3 almost full elixir collectors or half full maxed collectors
		
		System.out.println(t1.getName() + ": " + t1.numStrongMatches());
		System.out.println(t2.getName() + ": " + t2.numStrongMatches());
		
		return t1.numStrongMatches() + t2.numStrongMatches() >= 2;
				
	}
	
	public boolean shouldReallyAttack() {
		
		// take screenshot
		screen = concat(Main.getTopScreen(), Main.getBottomScreen());
		
		// update templates with screenshot
		for(Template t: templates.values()) {
			t.refresh(screen);
		}
		Template.removeOverlappingMatches(templates.values());

		// compute the score
		double score = 0;
		for(Template t: templates.values()) {
			score += t.getScore();
		}

		//System.out.println("shouldReallyAttack = " + score);
		
		return score >= 2.0;
	}
	
	/**
	 * Writes a log of the current base, including a screenshot and the match information.
	 * @param folder
	 * @param filenameBase
	 * @param matches
	 */
	public void logBase(String folder, String filenameBase) {
		
		try {
			// write raw image
			Graphics2D g2;
			RenderedImage rImage = writeImage(folder + "\\raw\\" + filenameBase + ".png", screen);
			
			
			// for writing match data, one file for all templates
			PrintWriter writer = new PrintWriter(folder + "\\" + filenameBase + ".log");
			
			// write image for each template
			for(Template t : templates.values()) {
				if(t.numStrongMatches() > 0) {
					// get blurred screenshot from template
					BufferedImage screenMarked = deepCopy(screen);
					g2 = screenMarked.createGraphics();
					g2.drawImage(screenMarked, null, null);
					rImage = (RenderedImage)screenMarked;
					
					// draw rectangles for this template's matches
					t.drawRectangles(g2);
					
					// write the image
					ImageIO.write(rImage, "PNG", new File(folder + "\\" + t.getName() + "\\" + filenameBase + ".png"));
				}
				
				// write match data for this template
				writer.write(t.toString());
			}
			
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private RenderedImage writeImage(String pathName,
			BufferedImage screen) throws IOException {
		Graphics2D g2 = screen.createGraphics();
		g2.drawImage(screen, null, null);
		RenderedImage rImage = (RenderedImage)screen;
		ImageIO.write(rImage, "PNG", new File(pathName));
		return rImage;
	}
	
	
	
	private static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	
	private BufferedImage concat(BufferedImage top, BufferedImage bottom) {
		
		int rows = 2;   //we assume the no. of rows and cols are known and each chunk has equal width and height  
        int cols = 1;  
        //int chunks = rows * cols;  
  
        int chunkWidth, chunkHeight;  
        int type;
        
        BufferedImage[] buffImages = new BufferedImage[2];
        buffImages[0] = top;
        buffImages[1] = bottom;
               
        type = buffImages[0].getType();  
        chunkWidth = buffImages[0].getWidth();  
        chunkHeight = buffImages[0].getHeight();  
  
        //Initializing the final image  
        BufferedImage finalImg = new BufferedImage(chunkWidth*cols, chunkHeight*rows, type);  
  
        int num = 0;  
        for (int i = 0; i < rows; i++) {  
            for (int j = 0; j < cols; j++) {  
                finalImg.createGraphics().drawImage(buffImages[num], chunkWidth * j, chunkHeight * i, null);  
                num++;  
            }  
        }  
        return finalImg;
	}
	
}
