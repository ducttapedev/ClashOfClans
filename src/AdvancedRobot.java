import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AdvancedRobot extends Robot {

	private int delayAfterMouseMove = 500;
	private int delayAfterMouseClick = 500;
	
	private static final int LEFT_MOUSE = InputEvent.BUTTON1_MASK;
	private static final int RIGHT_MOUSE = InputEvent.BUTTON3_MASK;
	
	public AdvancedRobot() throws AWTException {
		super();
	}
	
	public void quickestSpeed() {
		delayAfterMouseClick = 0;
		delayAfterMouseMove = 0;
	}
	
	public void quickSpeed() {
		delayAfterMouseClick = 40;
		delayAfterMouseMove = 40;
	}
	
	public void mediumSpeed() {
		delayAfterMouseClick = 200;
		delayAfterMouseMove = 200;
	}
	
	public void defaultSpeed() {
		delayAfterMouseClick = 500;
		delayAfterMouseMove = 500;
	}
	
	public void delay(int ms) {
		while(ms > 60*1000) {
			super.delay(60*1000);
			ms -= 60*1000;
		}
		super.delay(ms);
	}
	
	/**
	 * Moves the mouse to the specified point
	 * @param p
	 */
	public void mouseMove(Point p) {
		mouseMove(p.x, p.y);
		delay(delayAfterMouseMove);
	}
	
	
	/**
	 * Moves the mouse to the specified point
	 * @param p
	 * @param delay ms to wait after moving
	 */
	public void mouseMove(Point p, int delay) {
		mouseMove(p.x, p.y);
		delay(delay);
	}
	

	/**
	 * Clicks the left mouse button
	 */
	public void click() {
		mousePress(LEFT_MOUSE);     
		mouseRelease(LEFT_MOUSE);
		delay(delayAfterMouseClick);
	}
	
	public void rightClick() {
		mousePress(RIGHT_MOUSE);
		mouseRelease(RIGHT_MOUSE);
		delay(delayAfterMouseClick);
	}
	
	/**
	 * Moves to <code>startX, startY</code>, clicks, and drags to <code>endX, endY</code>
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 */
	public void clickAndDrag(int startX, int startY, int endX, int endY) {
		clickAndDrag(new Point(startX, startY), new Point(endX, endY));
	}
	
	
	/**
	 * Moves to <code>start</code>, clicks, and drags to <code>end</code>
	 * @param start
	 * @param end
	 */
	public void clickAndDrag(Point start, Point end) {
		mouseMove(start);
		mousePress(LEFT_MOUSE);
		delay(delayAfterMouseClick);
		mouseMove(end);
		mouseRelease(LEFT_MOUSE);
		delay(delayAfterMouseClick);
	}
	
	/**
	 * Moves to <code>start</code>, right clicks, and drags gradually to <code>end</code>
	 * @param start
	 * @param end
	 */
	public void rightClickAndDragGradually(Point start, Point end) {
		mouseMove(start);
		mousePress(RIGHT_MOUSE);
		delay(delayAfterMouseClick);
		
		// move the mosue gradually from left to right
		//List<Point> clickLocations = getClickLocations(start, end, );
		double distance = start.distance(end);
		double theta = Math.atan2(end.y - start.y, end.x - start.x);
		int numLocations = (int) Math.round(distance + 0.5);
		
		for(int i = 0; i < numLocations; i++) {
			int dx = (int) (i*Math.cos(theta));
			int dy = (int) (i*Math.sin(theta));
			Point p = new Point(start.x + dx, start.y + dy);
			mouseMove(p, 10);
		}
		
		//mouseMove(end);
		delay(2000);
		mouseRelease(RIGHT_MOUSE);
		delay(delayAfterMouseClick);
	}
	
	/**
	 * Moves the mouse to the specified point and left-clicks
	 * @param p
	 */
	public void moveAndClick(Point p) {
		mouseMove(p);
		click();
	}
	
	public void moveAndClickAndHold(Point p, int holdTime) {
		mouseMove(p);
		mousePress(LEFT_MOUSE);
		delay(holdTime);
		mouseRelease(LEFT_MOUSE);
		delay(delayAfterMouseClick);
	}
	
	
	
	
	/**
	 * Clicks <code>num</code> times, half between <code>p1</code> and <code>p2</code> and half between <code>p3</code> and <code>p4</code>.
	 * The point are uniformly distributed between the points, but clicked on in a random order.
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 * @param num
	 */
	public void clickBetween(Point p1, Point p2, Point p3, Point p4, int num) {
		List<Point> clickLocations = getClickLocations(p1, p2, num/2);
		clickLocations.addAll(getClickLocations(p3, p4, num - num/2));
		Collections.shuffle(clickLocations);
		
		for(int i = 0; i < clickLocations.size(); i++) {
			Point loc = clickLocations.get(i);
			moveAndClick(loc);
		}
	}

	private static final double RANDOM_SPACING_FACTOR = 1;
	
	/**
	 * @param p1
	 * @param p2
	 * @param num
	 * @return <code>num</code> click locations between <code>p1</code> and <code>p2</code>
	 */
	private static List<Point> getClickLocations(Point p1, Point p2, int num) {
		double slope = (p2.y - p1.y)*1.0/(p2.x - p1.x);
		double xDisplacement = p2.x - p1.x;
		double xStep = xDisplacement/(num+1);
		
		List<Point> clickLocations = new ArrayList<Point>();
		
		for(int i = 1; i <= num; i++) {
			double randomizedStep = i + 2*RANDOM_SPACING_FACTOR*Math.random() - RANDOM_SPACING_FACTOR;
			
			double x = p1.x + xStep*randomizedStep;
			double y = p1.y + xStep*slope*randomizedStep;
			clickLocations.add(new Point((int)x, (int)y));
		}
		return clickLocations;
	}

	
}
