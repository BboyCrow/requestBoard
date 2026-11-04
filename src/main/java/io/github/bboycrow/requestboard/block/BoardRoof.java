package io.github.bboycrow.requestboard.block;

import net.minecraft.util.StringRepresentable;

/** Stone used for the little roof, picked from the slab used in the recipe. */
public enum BoardRoof implements StringRepresentable {
	STONE("stone"),
	COBBLESTONE("cobblestone"),
	MOSSY_COBBLESTONE("mossy_cobblestone"),
	STONE_BRICKS("stone_bricks"),
	COBBLED_DEEPSLATE("cobbled_deepslate"),
	DEEPSLATE_BRICKS("deepslate_bricks"),
	BLACKSTONE("blackstone"),
	ANDESITE("andesite"),
	GRANITE("granite"),
	DIORITE("diorite"),
	SANDSTONE("sandstone"),
	RED_SANDSTONE("red_sandstone"),
	BRICKS("bricks"),
	MUD_BRICKS("mud_bricks"),
	TUFF("tuff"),
	NETHER_BRICKS("nether_bricks");

	private final String name;

	BoardRoof(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
