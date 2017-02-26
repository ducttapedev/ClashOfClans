import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;


public class CharacterRecognition {
	
	/**
	 * Reads the image for exact matches with the templates
	 * @param image
	 * @param targetColor
	 * @param templates
	 * @return the integer recognized
	 */
	public int readInteger(BufferedImage image, Color targetColor, List<boolean[][]> templates) {
		
		// convert image to binary based on target color
		boolean[][] binaryImage = new boolean[image.getWidth()][image.getHeight()];
		
		for(int x = 0; x < binaryImage.length; x++)
			for(int y = 0; y < binaryImage[x].length; y++) {
				binaryImage[x][y] = targetColor.getRGB() == image.getRGB(x, y);
			}
		
		// try to match the templates
		for(int x = 0; x < binaryImage.length; x++) {
			for(int y = 0; y < binaryImage[x].length; y++) {
				if(binaryImage[x][y]) {
					
				}
				
			}
		}
		
	
		return 0;
	}
	
}
