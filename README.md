Clash of Clans intelligent agent
Designed and implemented by Michael Cheung (https://github.com/michaelscheung)

This intelligent agent trains units, searches bases for large easily accesible resources stored in elixir pumps, attacks those bases, and repeats indefinitely. It has built in mechanisms to handle lag, the game freezing or crashing, disconnection, and forced breaks. It's not perfect, though. For instance, if the emulator itself crashes, you need to make a manual reset. There is also an option to have the bot send you an e-mail when something goes wrong (Modify the resetGame method in Main.java).

This intelligent agent has been tested and developed on the Genymotion (https://www.genymotion.com/) using device: Samsung Galaxy S4 - 4.3 - API 18 - 1080x1920, while maximized. However, the agent should work on any resolution, any emulator, and with any version of Clash of Clans unless there is a large revamp to the layout. UPDATE: The new way to train armies (with a single unified UI instead of separate screens for each barracks and spell factory) breaks the current implementation. I am no longer maintaining this project, but feel free to fork and contribute if you are interested :)


./username1.settings contains the settings for number of barbarians (numBarbs) and archers (numArchers) to use for the attack, the path for short form logs (logFolder) and long form logs (dataFolder), and files describing more settings:
	- baseAnalyzerFilename describes the images of elixir pumps by level and fullness.
	- windowAnalyzerFilename describes the images of various key elements in the UI. This is to tell the bot where to click for certain actions, and often as an indicator of what state the game is in.
	- coordsFilename describes the coordinates of various key elements in the UI. This is to tell the bot where to click for certain actions. Theoretically, this can be accomplished with the same template matching used on windowAnalyzer.prop, but since these elements don't change position, it's not necessary. Also, any coordinates that are not defined here default to their values in ./genericCoords.prop