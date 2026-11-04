package io.github.clementgen.requestboard.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum Category implements StringRepresentable {
	ANNOUNCEMENT("announcement", 0xFF4F9BD9),
	SERVICE("service", 0xFFA875D6),
	HARVEST("harvest", 0xFF62B357);

	public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);
	public static final StreamCodec<ByteBuf, Category> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[Math.floorMod(i, values().length)], Category::ordinal);

	private final String name;
	private final int color;

	Category(String name, int color) {
		this.name = name;
		this.color = color;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public int color() {
		return this.color;
	}

	public String translationKey() {
		return "requestboard.category." + this.name;
	}
}
