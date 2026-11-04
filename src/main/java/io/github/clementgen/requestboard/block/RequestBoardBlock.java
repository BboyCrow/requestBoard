package io.github.clementgen.requestboard.block;

import io.github.clementgen.requestboard.server.RequestManager;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RequestBoardBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<BoardWood> WOOD = EnumProperty.create("wood", BoardWood.class);
	public static final EnumProperty<BoardRoof> ROOF = EnumProperty.create("roof", BoardRoof.class);

	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
		Block.box(0, 0, 6, 2, 16, 10),   // left post
		Block.box(14, 0, 6, 16, 16, 10), // right post
		Block.box(2, 3, 7, 14, 14, 9),   // board
		Block.box(0, 14, 3, 16, 16, 13)  // roof
	));

	public RequestBoardBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(WOOD, BoardWood.OAK)
			.setValue(ROOF, BoardRoof.STONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WOOD, ROOF);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			RequestManager.openBoard(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return withVariant(new ItemStack(this), state.getValue(WOOD), state.getValue(ROOF));
	}

	/** Stores the wood and roof in the item so the placed block keeps them. */
	public static ItemStack withVariant(ItemStack stack, BoardWood wood, BoardRoof roof) {
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(WOOD, wood).with(ROOF, roof));
		return stack;
	}
}
