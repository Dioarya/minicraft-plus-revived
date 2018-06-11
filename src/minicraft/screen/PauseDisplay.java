package minicraft.screen;

import java.util.ArrayList;
import java.util.Arrays;

import minicraft.core.Game;
import minicraft.core.Network;
import minicraft.core.io.InputHandler;
import minicraft.core.io.Localization;
import minicraft.gfx.Color;
import minicraft.saveload.Save;
import minicraft.screen.entry.BlankEntry;
import minicraft.screen.entry.ListEntry;
import minicraft.screen.entry.SelectEntry;
import minicraft.screen.entry.StringEntry;

public class PauseDisplay extends Display {
	
	public PauseDisplay() {
		String upString = Game.input.getMapping("up")+ Localization.getLocalized(" and ")+Game.input.getMapping("down")+Localization.getLocalized(" to Scroll");
		String selectString = Game.input.getMapping("select")+Localization.getLocalized(": Choose");
		
		
		ArrayList<ListEntry> entries = new ArrayList<>();
		entries.addAll(Arrays.asList(
			new BlankEntry(),
			new SelectEntry("Return to Game", () -> Game.setMenu(null)),
			new SelectEntry("Options", () -> Game.setMenu(new OptionsDisplay()))
			));
		
		if(!Game.ISONLINE) {
			entries.add(new SelectEntry("Make World Multiplayer", () -> {
				Game.setMenu(null);
				Network.startMultiplayerServer();
			}));
		}
		
		if(!Game.isValidClient()) {
			entries.add(new SelectEntry("Save Game", () -> {
				Game.setMenu(null);
				if(!Game.isValidServer())
					new Save(WorldSelectDisplay.getWorldName());
				else
					Game.server.saveWorld();
			}));
		}
		
		entries.addAll(Arrays.asList(
			new SelectEntry("Main Menu", () -> Game.setMenu(new TitleDisplay())),
			
			new BlankEntry(),
			
			new StringEntry(upString, Color.GRAY),
			new StringEntry(selectString, Color.GRAY)
		));
		
		//Menu.Builder msgBuilder = new Menu.Builder(8);
		
		menus = new Menu[] {
			new Menu.Builder(true, 4, RelPos.CENTER, entries)
				.setTitle("Paused", 550)
				.createMenu()/*,
			
			msgBuilder.setEntries(new StringEntry("Save Game?"), new StringEntry("(Hint: Press \"r\" to save in-game)", Color.DARK_GRAY))
				.createMenu(),
			
			msgBuilder.setEntries(new StringEntry(""))*/
		};
	}
	
	@Override
	public void init(Display parent) {
		super.init(null); // ignore; pause menus always lead back to the game
	}
	
	@Override
	public void tick(InputHandler input) {
		super.tick(input);
		if (input.getKey("pause").clicked)
			Game.exitMenu();
	}
}
