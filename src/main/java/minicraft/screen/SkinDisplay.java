package minicraft.screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import minicraft.core.FileHandler;
import minicraft.core.Game;
import minicraft.core.Renderer;
import minicraft.core.io.InputHandler;
import minicraft.gfx.Color;
import minicraft.gfx.Font;
import minicraft.gfx.MobSprite;
import minicraft.gfx.Screen;
import minicraft.gfx.SpriteSheet;
import minicraft.saveload.Save;
import minicraft.screen.entry.ListEntry;
import minicraft.screen.entry.SelectEntry;

/**
 * The skins are put in a folder generated by the game called "skins".
 * Many skins can be put according to the number of files.
 */
public class SkinDisplay extends Display {
	private static final List<String> skinNames = new ArrayList<>();
	private static final int defaultSkins;
	private static final SpriteSheet defaultSheet;
	private static final List<String> customSkins = new ArrayList<>();
	private static int selectedSkinIndex = 0;
	private static SpriteSheet selectedSkinSheet;
	private static int tempSelection;

	private int step;

	static {
		// Load the default sprite sheet.
		defaultSheet = Renderer.loadDefaultSpriteSheets()[4];

		// These are all the generic skins. To add one, just add an entry in this list.
		skinNames.add("Paul");
		skinNames.add("Paul with cape");
		skinNames.add("Familiar boy");
		skinNames.add("Familiar girl");

		// Never remove this
		defaultSkins = skinNames.size();

		// Get the folder containing the skins.
		File skinsFolder = new File(FileHandler.getSystemGameDir() + "/" + FileHandler.getLocalGameDir() + "/skins");

		// Create folder, and see if it was successful.
		if (skinsFolder.mkdirs()) {
			Logger.info("Created resource packs folder at {}.", skinsFolder);
		}

		// Read and add the .png file to the skins list.
		for (String skinPath : Objects.requireNonNull(skinsFolder.list())) {
			if (skinPath.endsWith(".png")) {
				if (getSkinAsSheet(skinsFolder + "/" + skinPath) != null) {
					// Add the sprite sheet to the custom skins list.
					customSkins.add(skinsFolder + "/" + skinPath);

					// Remove the filetype (.png) and to the .
					skinNames.add(skinPath.substring(0, skinPath.length() - 4));
				}
			}
		}
	}

	public SkinDisplay() {
		super(true, true,
				new Menu.Builder(false, 2, RelPos.CENTER, getSkinsAsEntries()).setSize(48, 64).createMenu());
	}

	@Override
	public void init(@Nullable Display parent) {
		super.init(parent);

		menus[0].setSelection(selectedSkinIndex);
		tempSelection = selectedSkinIndex;
	}

	public static List<ListEntry> getSkinsAsEntries() {
		List<ListEntry> l = new ArrayList<>();
		for (String s : skinNames) {
			l.add(new SelectEntry(s, SkinDisplay::confirmExit));
		}

		return l;
	}

	private static SpriteSheet getSkinAsSheet(String path) {
		BufferedImage image;
		try {
			image = ImageIO.read(new FileInputStream(path));
		} catch (IOException e) {
			Logger.error("Could not read image at path {}. The file is probably missing or formatted wrong.", path);
			return null;
		} catch (SecurityException e) {
			Logger.error("Access to file located at {} was denied. Check if game is given permission.", path);
			return null;
		}

		// If we found an image.
		if (image != null) {
			SpriteSheet spriteSheet = new SpriteSheet(image);

			// Check if sheet is a multiple of 8.
			if (spriteSheet.width % 8 == 0 && spriteSheet.height % 8 == 0) {
				return spriteSheet;
			} else {
				// Go here if image has wrong dimensions.
				Logger.error("Custom skin at '{}' has incorrect width or height. Should be a multiple of 8.", path);
			}
		}

		return null;
	}

	/**
	 * If we exited by selecting a skin.
	 */
	private static void confirmExit() {
		Game.exitDisplay();

		// Achieve Fashion Show:
		AchievementsDisplay.setAchievement("minicraft.achievement.skin", true);

		// Tell the player to apply changes.
		if (Game.player != null) {
			Game.player.updateSprite();
		}

		// Save the selected skin.
		new Save();
		selectedSkinIndex = tempSelection;
	}


	@Override
	public void tick(InputHandler input) {
		super.tick(input);

		int prevSel = tempSelection;
		tempSelection = menus[0].getSelection();

		// Executes every time the selection is updated.
		if (tempSelection != prevSel) {
			if (tempSelection >= defaultSkins) {
				selectedSkinSheet = getSkinAsSheet(customSkins.get(tempSelection - defaultSkins));

				// Something failed when getting the sheet, so remove it from the list and set the skin back to default.
				if (selectedSkinSheet == null) {
					// Set selected skin back to default.
					selectedSkinIndex = 0;
					Renderer.screen.setSkinSheet(defaultSheet);

					// Remove references to the skin.
					customSkins.remove(tempSelection - defaultSkins);
					skinNames.remove(tempSelection);

					// Refresh menu and save.
					menus[0].setEntries(getSkinsAsEntries().toArray(new ListEntry[0]));
					init(getParent());
					new Save();

					Logger.error("Error setting skin. Removed skin from list and set skin back to default.");
					return;
				}

				Renderer.screen.setSkinSheet(selectedSkinSheet);
				Logger.debug("Skin sheet set to {}.png.", skinNames.get(tempSelection));
			} else {
				Renderer.screen.setSkinSheet(defaultSheet);
				Logger.debug("Skin sheet changed to default sheet.");
			}
		}
	}

	@Override
	public void render(Screen screen) {
		super.render(screen);
		step++;

		// Title.
		Font.drawCentered("Skins", screen, Screen.h - 180, Color.WHITE);

		int h = 2;
		int w = 2;
		int xOffset = Screen.w / 2 - w * 4; // Put this in the center of the screen
		int yOffset = 38;

		int spriteIndex = (step / 40) % 8; // 9 = 8 Frames for sprite

		// Render preview of skin.
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				if (menus[0].getSelection() < defaultSkins) {
					screen.render(xOffset + x * 8, yOffset + y * 8, spriteIndex * 2 + x + (y + menus[0].getSelection() * 4) * 32, 0, 4);
				} else {
					screen.render(xOffset + x * 8, yOffset + y * 8, spriteIndex * 2 + x + y * 32, 0, selectedSkinSheet, - 1, false, 0);
				}

		// Help text.
		Font.drawCentered("Use "+ Game.input.getMapping("cursor-down") + " and " + Game.input.getMapping("cursor-up") + " to move.", screen, Screen.h - 17, Color.DARK_GRAY);
		Font.drawCentered(Game.input.getMapping("SELECT") + " to select, and " + Game.input.getMapping("EXIT") + " to cancel." , screen, Screen.h - 9, Color.DARK_GRAY);
	}

	public static int getSelectedSkinIndex() {
		return selectedSkinIndex;
	}

	public static void setSelectedSkinIndex(int selectedSkinIndex) {
		SkinDisplay.selectedSkinIndex = selectedSkinIndex;
	}

	// First array is one of the four animations.
	@NotNull
	public static MobSprite[][][] getSkinAsMobSprite() {
		MobSprite[][][] mobSprites = new MobSprite[4][][];

		if (selectedSkinIndex < defaultSkins) {
			mobSprites[0] = MobSprite.compilePlayerSpriteAnimations(0, SkinDisplay.getSelectedSkinIndex() * 4);
			mobSprites[1] = MobSprite.compilePlayerSpriteAnimations(0, SkinDisplay.getSelectedSkinIndex() * 4 + 2);
			mobSprites[2] = MobSprite.compilePlayerSpriteAnimations(8, SkinDisplay.getSelectedSkinIndex() * 4);
			mobSprites[3] = MobSprite.compilePlayerSpriteAnimations(8, SkinDisplay.getSelectedSkinIndex() * 4 + 2);
		} else {
			mobSprites[0] = MobSprite.compilePlayerSpriteAnimations(0, 0);
			mobSprites[1] = MobSprite.compilePlayerSpriteAnimations(0, 2);
			mobSprites[2] = MobSprite.compilePlayerSpriteAnimations(8, 0);
			mobSprites[3] = MobSprite.compilePlayerSpriteAnimations(8, 2);
		}

		return mobSprites;
	}
}
