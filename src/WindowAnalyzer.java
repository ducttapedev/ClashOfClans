import java.awt.Point;
import java.util.Map;

import boofcv.struct.feature.Match;


public class WindowAnalyzer {
	private Map<String, Template> templateList;
	
	public WindowAnalyzer(Map<String, Template> templateList) {
		this.templateList = templateList;
	}
	
	public boolean isCurrentWindow(String name) {
		Template t = templateList.get(name);
		// refresh the template with the current screen
		t.refresh(Main.getScreenshot());
		
		return t.numStrongMatches() > 0;
	}
	
	public Point getPointOfMatch(String name) {
		// refresh the template with the current screen
		Template t = templateList.get(name);
		t.refresh(Main.getScreenshot());
		
		Match m = t.getStrongMatch();
		if(m == null)
			return null;
		return new Point(m.x, m.y);
	}
	
	public Point getCenterOfMatch(String name) {
		Point p = getPointOfMatch(name);
		if(p == null)
			return null;
		Template t = templateList.get(name);
		int x = p.x + t.getImageWidth()/2;
		int y = p.y + t.getImageHeight()/2;
		return new Point(x, y);
	}
	
	public Template getCurrentWindow() {
		for(Template t: templateList.values()) {
			t.refresh(Main.getScreenshot());
			if(t.numStrongMatches() > 0)
				return t;
		}
		return null;
	}
	
	//public 
}
