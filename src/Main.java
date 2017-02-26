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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;


public class Main {
	//private static final File COLOR_FILE = new File("colors.prop");
	private static final File GENERIC_COORDS_FILE = new File("genericCoords.prop");
	private static final Properties genericCoordsProp = getGenericCoordsProp();
	
	private static Properties getGenericCoordsProp() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(GENERIC_COORDS_FILE));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prop;
	}
	
	public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private final String logFolder;
	private final String fullLogsFolder;
	
	private final Properties coordsProp = new Properties(),
			baseProp = new Properties(),
			windowProp = new Properties();
	
	private final BaseAnalyzer baseAnalyzer;
	private final WindowAnalyzer windowAnalyzer;
	
	private static AdvancedRobot bot;
	
	private final int numBarbs, numArchers;
	private int numBases = 0;
	
	public static void main(String[] args) throws Exception {
		//String email = "5138021963@txt.att.net";
		bot = new AdvancedRobot();
		printMouseCoordinatesAndColor(500);
		
		botIndefinitely(new File("username1.settings"));
		
	}

	private static void botIndefinitely(File settings) throws IOException, AWTException,
			InterruptedException {
		Main tester = new Main(settings);
		
		Thread.sleep(2000);
		LOGGER.info("Initiazlied bot with settings from " + settings.getAbsolutePath());
		LOGGER.info("Current date and time = " + getDate());
		
		//zoomOut();
		tester.run();
		//tester.baseSearch();
		//tester.deployHeroes();
	}
	
	public void test() {
		bot.moveAndClick(getPoint("downRight1"));
	}

	public Main(File settingsFile) throws IOException, AWTException {
		// load settings file
		Properties settings = new Properties();
		settings.load(new FileInputStream(settingsFile));
		
		// load coordinates file
		File coordsFile = new File(settings.getProperty("coordsFilename"));
		coordsProp.load(new FileInputStream(coordsFile));
		
		// load base analyzer file
		File baseAnalyzerFile = new File(settings.getProperty("baseAnalyzerFilename"));
		baseProp.load(new FileInputStream(baseAnalyzerFile));
		
		// load window analyzer file
		File windowAnalyzerFile = new File(settings.getProperty("windowAnalyzerFilename"));
		windowProp.load(new FileInputStream(windowAnalyzerFile));
		
		// set logging paths
		logFolder = settings.getProperty("logFolder");
		fullLogsFolder = settings.getProperty("dataFolder");
		
		// initialize robot
		bot = new AdvancedRobot();
		
		// set attacking force
		numBarbs = Integer.parseInt(settings.getProperty("numBarbs"));
		numArchers = Integer.parseInt(settings.getProperty("numArchers"));
		
		// get all templates
		Map<String, Template> baseTemplates = getTemplates(baseProp);
		Map<String, Template> windowTemplates = getTemplates(windowProp);
		
		// initialize analyzers
		baseAnalyzer = new BaseAnalyzer(baseTemplates);
		windowAnalyzer = new WindowAnalyzer(windowTemplates);
		
		// create logging folders if they don't already exist
		new File(logFolder + "\\raw").mkdirs();
		for(Template t: baseTemplates.values()) {
			new File(logFolder + "\\" + t.getName()).mkdirs();
		}
		
		// create data folders if they don't already exist
		new File(fullLogsFolder + "\\raw").mkdirs();
		for(Template t: baseTemplates.values()) {
			new File(fullLogsFolder + "\\" + t.getName()).mkdirs();
		}
		
		Handler handler = new FileHandler(logFolder + "\\CONSOLE" + getDate() + ".log");
		handler.setLevel(Level.ALL);
		Logger.getLogger("").addHandler(handler);
		SimpleFormatter formatter = new SimpleFormatter();
		handler.setFormatter(formatter);
		
		LOGGER.info("Logging screenshots of bases attacked to " + new File(
				logFolder).getAbsolutePath());
		LOGGER.info("Logging screenshots of bases attacked to " + new File(
				fullLogsFolder).getAbsolutePath());
	}
	
	
	private void run() {
		// all reference images were taken zoomed all the way out
		//zoomOut();

		/*
		System.out.println("shouldReallyAttack = " + baseAnalyzer.shouldReallyAttack());
		baseAnalyzer.logBase(logFolder, "test");
		System.exit(0);
		*/
		
		while(true) {
			// wait until returning (look for the "attack" button at the bottom-left)
			while(!windowAnalyzer.isCurrentWindow("home")) {
				checkIfKicked(windowAnalyzer);
				LOGGER.info("waiting to return home");
			}
			bot.delay(1000);
			
			// train troops upon return
			trainTroops();
						
			// wait until army camp fills
			waitForFullArmyCamp(windowAnalyzer);
			//System.exit(0);
				
			// train troops before leaving to attack
			trainTroops();
	
			baseSearch();			
				
		}
	}

	private void delay(int ms) {
		bot.delay(ms);
	}
	
	/**
	 * Checks if we have been kicked from the game to take a break or due to inactivity, and reloads the game if so.
	 * @param windowAnalyzer
	 * @return true if we were kicked
	 */
	private boolean checkIfKicked(WindowAnalyzer windowAnalyzer) {
		Point p = null;
		boolean kicked = false;
		String kickedReason = null;
		String takeABreak = "take a break", inactive = "inactive", disconnected = "disconnected";
		long start = System.nanoTime();
		
		do {
			// if we are asked to take a break, click "reload"
			p = windowAnalyzer.getPointOfMatch(takeABreak);
			if(p != null) {
				kickedReason = takeABreak;
			}
			
			// otherwise, if we are kicked due to inactivity, click "reload game"
			else {
				p = windowAnalyzer.getPointOfMatch(inactive);
				if(p != null) {
					kickedReason = inactive;
				}
				// otherwise, if we are kicked due to disconnection, click "try again"
				else {
					p = windowAnalyzer.getPointOfMatch(disconnected);
					if(p != null) {
						kickedReason = disconnected;
					}
				}
			}
			
			
			// if p is not null, then we were kicked for one reason or another. wait until we return home
			if(p != null) {
				kicked = true;
				assert kickedReason != null;
				LOGGER.warning("Clash of clans quit because " + kickedReason);
				LOGGER.warning("Attempting to reload");
				bot.moveAndClick(p);
				p = null;
			}
			
			if(kicked)
				LOGGER.warning("Waiting to return home");
			
			if(System.nanoTime() - start > 15*60e9)
				resetGame();
		
		// if we haven't been kicked to begin with, return
		// otherwise, continue this loop until we're back home
		} while(!windowAnalyzer.isCurrentWindow("home") && kicked);
		
		// if we were kicked and reloaded, zoom all the way out, then drag to the top
		if(kicked) {
			LOGGER.warning("Resetting to zoomed out and dragged up after reload");
			zoomOut();
			dragUp();
		}
		
		return kicked;
	}


	private void resetGame() {
		LOGGER.severe("Requested a game reset, messaging human...");
		try {
			GoogleMail.Send("username", "password", "target@email.com", "subject", "text");
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
		/*
		do {
			LOGGER.warning("Resetting game");
			System.out.println(windowAnalyzer.getPointOfMatch("restore"));
			System.out.println(windowAnalyzer.getCenterOfMatch("android home"));
			System.out.println(windowAnalyzer.getPointOfMatch("launch"));
			System.out.println(windowAnalyzer.getPointOfMatch("maximize"));
			
			bot.moveAndClick(windowAnalyzer.getPointOfMatch("restore"));
			bot.moveAndClick(windowAnalyzer.getCenterOfMatch("android home"));
			bot.moveAndClick(windowAnalyzer.getPointOfMatch("launch"));
			bot.moveAndClick(windowAnalyzer.getPointOfMatch("maximize"));
			
		} while(!windowAnalyzer.isCurrentWindow("home"));
		*/
		//TODO: what if we get attacked?
	}

	/**
	 * Uses the generic templates by default, which can be overridden in the user specified file
	 * @param properties
	 * @return map of template names to templates
	 */
	private static Map<String, Template> getTemplates(Properties properties) {
		Map<String, Template> baseAnalyzerTemplates = new HashMap<String, Template>();
		for(Object obj : properties.values()) {
			String s = (String)obj;
			Template t = Template.createTemplate(s);
			baseAnalyzerTemplates.put(t.getName(), t);
		}
		return baseAnalyzerTemplates;
	}


	/**
	 * Trains troops in the barracks. Completes unconditionally.
	 */
	private void trainTroops() {
		LOGGER.info("Training troops...");
		
		Point barracks = getPoint("barracks");
		Point train = getPoint("train");
		Point archer = getPoint("archer");
		Point barb = getPoint("barb");
		Point nextTrain = getPoint("nextTrain");
		
		dragUp();
		bot.moveAndClick(barracks);
		bot.moveAndClick(train);
		
		bot.moveAndClickAndHold(archer, 4*1000);
		bot.moveAndClick(nextTrain);
		bot.moveAndClickAndHold(archer, 4*1000);
		bot.moveAndClick(nextTrain);
		bot.moveAndClickAndHold(barb, 4*1000);
		bot.moveAndClick(nextTrain);
		bot.moveAndClickAndHold(barb, 4*1000);
		// click outside to quit out of the training screen
		bot.moveAndClick(getPoint("attack"));
		
		LOGGER.info("Done training troops!");
	}
	
	
	private boolean goHome(WindowAnalyzer analyzer) {
		
		//TODO: take care of d/c, forced breaks, and lag
		
		return false;
	}


	/**
	 * Waits until the army camp is full. Completion depends on the army camp being filled.
	 * @param analyzer
	 */
	private void waitForFullArmyCamp(WindowAnalyzer analyzer) {
		LOGGER.info("Waiting for army camp to fill...");
		bot.moveAndClick(getPoint("armyCamp"));
		bot.moveAndClick(getPoint("info"));
		long trainingStart = System.nanoTime();
		
		while(!analyzer.isCurrentWindow("full army camp")) {
			// check if we've been booted from the game to prevent being locked in this loop forever
			if(checkIfKicked(analyzer)) {
				bot.moveAndClick(getPoint("armyCamp"));
				bot.moveAndClick(getPoint("info"));
			}
			
			// every minute, click on the army camp again
			// so we don't get disconnected for inactivity
			else if(System.nanoTime() - trainingStart > 60e9) {
				bot.moveAndClick(getPoint("attack"));
				bot.moveAndClick(getPoint("info"));
				trainingStart = System.nanoTime();
			}
		}
		LOGGER.info("Done filling army camp!");
		
		bot.moveAndClick(getPoint("attack"));
		bot.delay(1000);
		
	}


	/**
	 * @param battleImage
	 * @param baseAnalyzer
	 * @param numBases
	 */
	private void baseSearch() {
		// find our first base
		bot.moveAndClick(getPoint("attack"));
		bot.moveAndClick(getPoint("findMatch"));
		
		long start = System.nanoTime();
		
		// wait until we are in battle
		while(!windowAnalyzer.isCurrentWindow("battle end")) {
			// if we have a shield, disable it
			if(windowAnalyzer.isCurrentWindow("shield"))
				bot.moveAndClick(getPoint("disableShield"));
			
			if(checkIfKicked(windowAnalyzer))
				return;
			
			if(System.nanoTime()-start > 400e9) {
				resetGame();
				return;
			}
		}
		
		int numSkipped = 0;
		
		while(true) {
			
			// if so, attack this base
			//baseAnalyzer.shouldAttack();
			boolean shouldAttack = baseAnalyzer.shouldReallyAttack();
			if(shouldAttack) {
				numSkipped = 0;
				
				// save a screenshot and the match data
				String baseFilename = getDate() + "_" + numBases;
				LOGGER.info("Attacking this base (" + numBases + " so far)...");
				LOGGER.info("Saving image data for base to " + baseFilename);
				baseAnalyzer.logBase(logFolder, baseFilename);
				baseAnalyzer.logBase(fullLogsFolder, baseFilename);
				numBases++;
				
				//TODO: detect and respond to faggotry
				bot.delay(20*1000);
				
				// execute attack
				surroundAttack();
				LOGGER.info("Done attacking base!");
				
				// return to base after 3 minutes or the post-battle screen appears
				start = System.nanoTime();
				while(!windowAnalyzer.isCurrentWindow("post-battle")
						&& System.nanoTime()-start < 3*60e9) {
					// if we get kicked, then stop waiting for the battle to end and just reload
					if(checkIfKicked(windowAnalyzer))
						return;
				}
				bot.moveAndClick(getPoint("returnHome"));
				return;

			}
			// otherwise skip the base
			else {
				LOGGER.info("Skipping this base");
				//baseAnalyzer.logBase(fullLogsFolder, getDate() + "_" + numSkipped++);
				bot.moveAndClick(getPoint("nextMatch"));
				
				// wait until we can see the "end match" button, as we may still be in the clouds
				//TODO: if we are in the clouds for more than 30 seconds, return to base
				
				start = System.nanoTime();
				do {
					LOGGER.fine("Waiting for new base to load...");
					bot.delay(200);
					
					// after waiting 30 seconds, check if we've been kicked
					// if we get kicked, then reload and stop searching for bases
					if(System.nanoTime()-start > 30e9)
						if(checkIfKicked(windowAnalyzer)) {
							return;
						}
					
					if(System.nanoTime()-start > 400e9) {
						resetGame();
						return;
					}
				} while(!windowAnalyzer.isCurrentWindow("battle end"));
				LOGGER.fine("Done waiting for new base to load!");
			}
						
		}
	}
	
	private void surroundAttack() {
		LOGGER.fine("Deploying troops from the top");
		dragUp();
		selectUnit(1);
		attackFromTop(numBarbs/2);
		selectUnit(2);
		attackFromTop(numArchers/2);
		bot.delay(500);
		
		LOGGER.fine("Deploying troops from the bottom");
		dragDown();
		selectUnit(1);
		attackFromBottom(numBarbs*3/5);
		selectUnit(2);
		attackFromBottom(numArchers*3/5);
		
		bot.delay(500);
		deployHeroes();
	}

	private void deployHeroes() {
		// deploy king if available
		Point kingPoint = windowAnalyzer.getCenterOfMatch("king");
		if(kingPoint != null) {
			LOGGER.fine("Deploying King");
			bot.moveAndClick(kingPoint);
			bot.moveAndClick(getPoint("downLeft1"));
		}
		
		// deploy queen if available
		Point queenPoint = windowAnalyzer.getCenterOfMatch("queen");
		if(queenPoint != null) {
			LOGGER.fine("Deploying Queen");
			bot.moveAndClick(queenPoint);
			bot.moveAndClick(getPoint("downRight1"));
		}

		// activate queen ability after 10 s
		bot.delay(10000);
		if(queenPoint != null) {
			bot.moveAndClick(queenPoint);
		}
		
		// activate king ability after 16 s
		bot.delay(6000);
		if(kingPoint != null) {
			bot.moveAndClick(kingPoint);
		}
	}
	
	/**
	 * Selects the unit (1~8) in battle
	 * @param index
	 */
	private void selectUnit(int index) {
		if(index < 1 || index > 8)
			return;
		
		bot.moveAndClick(getPoint("unit" + index));
	}

	/**
	 * Drops <code>num</code> troops from the top-left and top-right
	 * @param num
	 */
	private void attackFromTop(int num) {
		bot.quickSpeed();
		bot.clickBetween(getPoint("upLeft1"), getPoint("upLeft2"), getPoint("upRight1"), getPoint("upRight2"), num);
		bot.defaultSpeed();
	}

	/**
	 * Drops <code>num</code> troops from the bottom-left and bottom-right
	 * @param num
	 */
	private void attackFromBottom(int num) {
		bot.quickSpeed();
		bot.clickBetween(getPoint("downLeft1"), getPoint("downLeft2"), getPoint("downRight1"), getPoint("downRight2"), num);
		bot.defaultSpeed();
	}

	
	/**
	 * Drags the view to the top of the screen
	 */
	private static void dragUp() {
		Point top = getGenericPoint("screenUp");
		Point bottom = getGenericPoint("screenDown");
		bot.mediumSpeed();
		bot.clickAndDrag(top, bottom);
		bot.defaultSpeed();
	}
	
	/**
	 * Drags the view to the bottom of the screen
	 */
	private static void dragDown() {
		Point top = getGenericPoint("screenUp");
		Point bottom = getGenericPoint("screenDown");
		bot.mediumSpeed();
		bot.clickAndDrag(bottom, top);
		bot.defaultSpeed();
	}
	
	private static void zoomOut() {
		Point left = getGenericPoint("screenLeft");
		Point right = getGenericPoint("screenRight");
		Rectangle refRectangle = new Rectangle(351,186,50,50);
		BufferedImage refImage = getScreenshot(refRectangle);
		BufferedImage refImagePrev = null;
		
		while( !bufferedImagesEqual(refImage, refImagePrev) ) {
			System.out.println("Zooming out...");
			bot.rightClickAndDragGradually(left, right);
			refImagePrev = refImage;
			refImage = getScreenshot(refRectangle);
		}
		
	}
	
	public static boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
		if (img1 == null || img2 == null)
			return false;
	    if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
	        for (int x = 0; x < img1.getWidth(); x++) {
	            for (int y = 0; y < img1.getHeight(); y++) {
	                if (img1.getRGB(x, y) != img2.getRGB(x, y))
	                    return false;
	            }
	        }
	    } else {
	        return false;
	    }
	    return true;
	}
	
	/**
	 * @return screenshot of the current display
	 */
	public static BufferedImage getScreenshot() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenRectangle = new Rectangle(screenSize);
		return bot.createScreenCapture(screenRectangle);
	}
	
	/**
	 * @param rect
	 * @return screenshot of the current display for the specified rectangle
	 */
	public static BufferedImage getScreenshot(Rectangle rect) {
		return bot.createScreenCapture(rect);
	}

	public static BufferedImage getTopScreen() {
		dragUp();
		return bot.createScreenCapture(getGenericRectangle("topScreen"));
	}
	
	public static BufferedImage getBottomScreen() {
		dragDown();
		return bot.createScreenCapture(getGenericRectangle("bottomScreen"));
	}
	
	/**
	 * @param name
	 * @return a <code>Point<code> object containing the coordinates of <code>name</code> as defined by the general properties file
	 */
	private Point getPoint(String name) {
		int[] coords = getIntegers(name);
		return new Point(coords[0], coords[1]);
	}
	
	private Color getColor(String name) {
		int[] comp = getIntegers(name);
		return new Color(comp[0], comp[1], comp[2]);
	}
	
	private Rectangle getRectangle(String name) {
		int[] params = getIntegers(name);
		return new Rectangle(params[0], params[1], params[2], params[3]);
	}
	
	private int[] getIntegers(String name) {
		//System.out.println("POINT: " + name);
		String property = coordsProp.getProperty(name);
		// TODO: change all generic calls to use the generic method
		if(property == null)
			property = genericCoordsProp.getProperty(name);
		
		String[] stringIntegers = property.split(",");
		int[] parsedIntegers = new int[stringIntegers.length];
		
		for(int i = 0; i < parsedIntegers.length; i++)
			parsedIntegers[i] = Integer.parseInt(stringIntegers[i]);
		
		return parsedIntegers;
	}
	
	private static Point getGenericPoint(String name) {
		int[] coords = getIntegers(genericCoordsProp, name);
		return new Point(coords[0], coords[1]);
	}
	
	private static Rectangle getGenericRectangle(String name) {
		int[] params = getIntegers(genericCoordsProp, name);
		return new Rectangle(params[0], params[1], params[2], params[3]);
	}

	/*
	private static Point getPoint(Properties prop, String name) {
		int[] coords = getIntegers(prop, name);
		return new Point(coords[0], coords[1]);
	}
	
	private static Color getColor(Properties prop, String name) {
		int[] comp = getIntegers(prop, name);
		return new Color(comp[0], comp[1], comp[2]);
	}
	
	private static Rectangle getRectangle(Properties prop, String name) {
		int[] params = getIntegers(prop, name);
		return new Rectangle(params[0], params[1], params[2], params[3]);
	}
	*/
	private static int[] getIntegers(Properties prop, String name) {
		//System.out.println("POINT: " + name);
		String[] stringIntegers = prop.getProperty(name).split(",");
		int[] parsedIntegers = new int[stringIntegers.length];
		
		for(int i = 0; i < parsedIntegers.length; i++)
			parsedIntegers[i] = Integer.parseInt(stringIntegers[i]);
		
		return parsedIntegers;
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

	/*
	 * load the properties file
	 *
	private static void initProperties() {
		// load properties file
		coordsProp = new Properties();
		baseProp = new Properties();
		windowProp = new Properties();
		try {
			FileInputStream inStream = new FileInputStream(POINTS_FILE);
			coordsProp.load(inStream);
			
			inStream = new FileInputStream(BASE_ANALYZER_FILE);
			baseProp.load(inStream);
			
			inStream = new FileInputStream(WINDOW_ANALYZER_FILE);
			windowProp.load(inStream);
			
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not load properties file(s)");
		}
	}
	*/

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
	
	public static String getDate() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		sdf.setTimeZone(TimeZone.getTimeZone("EST"));
		String formattedDate = sdf.format(date);
		return formattedDate;
	}
}

