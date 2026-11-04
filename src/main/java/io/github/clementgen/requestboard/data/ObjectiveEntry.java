package io.github.clementgen.requestboard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A requested item and the total amount wanted (may exceed a stack). */
public record ObjectiveEntry(Holder<Item> item, int count) {
	public static final Codec<ObjectiveEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
		BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(ObjectiveEntry::item),
		Codec.INT.fieldOf("count").forGetter(ObjectiveEntry::count)
	).apply(i, ObjectiveEntry::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ObjectiveEntry> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.holderRegistry(Registries.ITEM), ObjectiveEntry::item,
		ByteBufCodecs.VAR_INT, ObjectiveEntry::count,
		ObjectiveEntry::new
	);

	public ItemStack displayStack() {
		return new ItemStack(this.item);
	}

	public int maxStackSize() {
		return Math.max(1, this.displayStack().getMaxStackSize());
	}
}
