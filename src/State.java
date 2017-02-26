import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;


public class State {
	private String name;
	private BufferedImage image;
	private float maxScore;
	
	public State(String name, BufferedImage image, float maxScore) {
		this.name = name;
		this.image = image;
		this.maxScore = maxScore;
	}
	
	public static State getState(String s) {
		try {
			String[] params = s.split(", ");
	
			String name = params[0];
			BufferedImage image = ImageIO.read(new File(params[1]));
			float maxScore = Float.parseFloat(params[2]);
			
			return new State(name, image, maxScore);
			
		} catch(Exception e) {
			return null;
		}
	}
}
