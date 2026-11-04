package io.github.clementgen.requestboard.block;

import io.github.clementgen.requestboard.server.RequestManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=1.21.2 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else
/*import net.minecraft.world.level.LevelAccessor;*/
//? if <26.1
/*import com.mojang.serialization.MapCodec;*/

/**
 * The notice board. Boards facing the same way merge with their neighbours (left, right, above, below)
 * into one bigger board: posts, roof and edges only appear on the outside.
 */
public class RequestBoardBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<BoardWood> WOOD = EnumProperty.create("wood", BoardWood.class);
	public static final EnumProperty<BoardRoof> ROOF = EnumProperty.create("roof", BoardRoof.class);
	/** Connected on the model's x=0 side (the facing's counter-clockwise neighbour). */
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	/** Connected on the model's x=16 side (the facing's clockwise neighbour). */
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");

	//? if <26.1
	/*public static final MapCodec<RequestBoardBlock> CODEC = simpleCodec(RequestBoardBlock::new);*/

	private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	public RequestBoardBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(WOOD, BoardWood.OAK)
			.setValue(ROOF, BoardRoof.STONE)
			.setValue(LEFT, false)
			.setValue(RIGHT, false)
			.setValue(UP, false)
			.setValue(DOWN, false));
	}

	//? if <26.1 {
	/*@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WOOD, ROOF, LEFT, RIGHT, UP, DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return connect(state, context.getLevel(), context.getClickedPos());
	}

	//? if >=1.21.2 {
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return connect(state, level, pos);
	}
	//?} else {
	/*@Override
	protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, LevelAccessor level,
			BlockPos pos, BlockPos neighbourPos) {
		return connect(state, level, pos);
	}
	*///?}

	/** Recomputes the four connections from the neighbours. */
	public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state
			.setValue(LEFT, connects(facing, level.getBlockState(pos.relative(facing.getCounterClockWise()))))
			.setValue(RIGHT, connects(facing, level.getBlockState(pos.relative(facing.getClockWise()))))
			.setValue(UP, connects(facing, level.getBlockState(pos.above())))
			.setValue(DOWN, connects(facing, level.getBlockState(pos.below())));
	}

	private static boolean connects(Direction facing, BlockState neighbour) {
		return neighbour.getBlock() instanceof RequestBoardBlock && neighbour.getValue(FACING) == facing;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean left = state.getValue(LEFT);
		boolean right = state.getValue(RIGHT);
		boolean up = state.getValue(UP);
		boolean down = state.getValue(DOWN);
		Direction facing = state.getValue(FACING);
		int key = facing.get2DDataValue() << 4 | (left ? 8 : 0) | (right ? 4 : 0) | (up ? 2 : 0) | (down ? 1 : 0);
		return SHAPES.computeIfAbsent(key, k -> {
			VoxelShape shape = box(facing, left ? 0 : 2, down ? 0 : 2, 7, right ? 16 : 14, up ? 16 : 14, 9);
			int postTop = up ? 16 : 14;
			if (!left) shape = Shapes.or(shape, box(facing, 0, 0, 6, 2, postTop, 10));
			if (!right) shape = Shapes.or(shape, box(facing, 14, 0, 6, 16, postTop, 10));
			if (!up) shape = Shapes.or(shape, box(facing, 0, 14, 3, 16, 16, 13));
			return shape;
		});
	}

	/** A box given for a north-facing board, turned to face the given direction (same turn as the blockstate models). */
	private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (facing) {
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			RequestManager.openBoard(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	//? if >=1.21.5 {
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return withVariant(new ItemStack(this), state.getValue(WOOD), state.getValue(ROOF));
	}
	//?} else {
	/*@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return withVariant(new ItemStack(this), state.getValue(WOOD), state.getValue(ROOF));
	}
	*///?}

	/** Stores the wood and roof in the item so the placed block keeps them. */
	public static ItemStack withVariant(ItemStack stack, BoardWood wood, BoardRoof roof) {
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(WOOD, wood).with(ROOF, roof));
		return stack;
	}
}
