package io.github.clementgen.requestboard.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.clementgen.requestboard.RequestBoard;
import io.github.clementgen.requestboard.config.RBConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Look of the board screen. Each player picks one; the server config gives the default. */
public enum BoardStyle {
	MODERN("textures/gui/board.png", 340, 240),
	FANTASY("textures/gui/board_fantasy.png", 680, 480);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static @Nullable BoardStyle chosen;
	private static boolean loaded;

	public final Identifier background;
	public final int textureWidth;
	public final int textureHeight;

	BoardStyle(String background, int textureWidth, int textureHeight) {
		this.background = RequestBoard.id(background);
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	public String translationKey() {
		return "requestboard.style." + this.name().toLowerCase(Locale.ROOT);
	}

	public BoardStyle next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(RequestBoard.MOD_ID + "-client.json");
	}

	/** The player's choice, or the server's default style. */
	public static BoardStyle current() {
		if (!loaded) {
			loaded = true;
			try {
				if (Files.exists(path())) {
					JsonObject json = GSON.fromJson(Files.readString(path(), StandardCharsets.UTF_8), JsonObject.class);
					if (json != null && json.has("style")) {
						chosen = parse(json.get("style").getAsString());
					}
				}
			} catch (Exception e) {
				RequestBoard.LOGGER.warn("Could not read {}", path(), e);
			}
		}
		if (chosen != null) {
			return chosen;
		}
		BoardStyle fallback = parse(RBConfig.get().defaultStyle);
		return fallback == null ? MODERN : fallback;
	}

	public static void choose(BoardStyle style) {
		chosen = style;
		loaded = true;
		try {
			JsonObject json = new JsonObject();
			json.addProperty("style", style.name());
			Files.writeString(path(), GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (Exception e) {
			RequestBoard.LOGGER.warn("Could not write {}", path(), e);
		}
	}

	public static @Nullable BoardStyle parse(@Nullable String name) {
		if (name == null) return null;
		try {
			return valueOf(name.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
