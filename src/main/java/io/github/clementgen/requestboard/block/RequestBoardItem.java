package io.github.clementgen.requestboard.block;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
//? if >=1.21.5
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Board item: the tooltip shows which wood and stone it is made of. */
public class RequestBoardItem extends BlockItem {
	public RequestBoardItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static BoardWood wood(ItemStack stack) {
		BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
		BoardWood wood = props.get(RequestBoardBlock.WOOD);
		return wood == null ? BoardWood.OAK : wood;
	}

	public static BoardRoof roof(ItemStack stack) {
		BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
		BoardRoof roof = props.get(RequestBoardBlock.ROOF);
		return roof == null ? BoardRoof.STONE : roof;
	}

	//? if >=1.21.5 {
	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
		Consumer<Component> builder = lines::add;
	*///?}
		builder.accept(Component.translatable("requestboard.tooltip.wood",
			Component.translatable("requestboard.wood." + wood(stack).getSerializedName())).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("requestboard.tooltip.roof",
			Component.translatable("requestboard.roof." + roof(stack).getSerializedName())).withStyle(ChatFormatting.GRAY));
	}
}
