package io.github.clementgen.requestboard.block;

import net.minecraft.util.StringRepresentable;

/** Wood used for the posts and the board. Textures come from vanilla, so each wood keeps its own colour. */
public enum BoardWood implements StringRepresentable {
	OAK("oak"),
	SPRUCE("spruce"),
	BIRCH("birch"),
	JUNGLE("jungle"),
	ACACIA("acacia"),
	DARK_OAK("dark_oak"),
	MANGROVE("mangrove"),
	CHERRY("cherry"),
	PALE_OAK("pale_oak"),
	CRIMSON("crimson"),
	WARPED("warped"),
	BAMBOO("bamboo");

	private final String name;

	BoardWood(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
