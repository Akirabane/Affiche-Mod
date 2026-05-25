package com.vikingposter.blocks;

import com.vikingposter.blockentity.PosterBlockEntity;
import com.vikingposter.items.SpongeEraserItem;
import com.vikingposter.network.NetworkHandler;
import com.vikingposter.network.S2COpenEditPacket;
import com.vikingposter.network.S2COpenQTEPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class PosterBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0,  16, 16, 1);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 0,  1,  16, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(15, 0, 0, 16, 16, 16);

    public PosterBlock() {
        super(BlockBehaviour.Properties.of()
            .noCollission()
            .strength(0.5f)
            .sound(SoundType.WOOD)
            .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float base = super.getDestroyProgress(state, player, level, pos);
        if (!(level.getBlockEntity(pos) instanceof PosterBlockEntity be)) return base;

        String url = be.getImageUrl();
        boolean hasImage = url != null && !url.isBlank();
        if (!hasImage) return base;

        // Hand-break time scales with surface area, capped so 16x16 stays feasible.
        float area  = be.getDisplayWidth() * be.getDisplayHeight();
        float ratio = Math.min(40f, Math.max(1f, area));
        if (player.getMainHandItem().getItem() instanceof AxeItem) ratio *= 0.5f;

        return base / ratio;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        if (clicked.getAxis().isVertical()) return null;
        return this.defaultBlockState().setValue(FACING, clicked);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wallPos = pos.relative(facing.getOpposite());
        return level.getBlockState(wallPos).isFaceSturdy(level, wallPos, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (dir == facing.getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player) {
            if (level.getBlockEntity(pos) instanceof PosterBlockEntity be) {
                be.setOwnerUUID(player.getUUID());
                be.setOwnerName(player.getName().getString());
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        BlockEntity raw = level.getBlockEntity(pos);
        if (!(raw instanceof PosterBlockEntity be)) return InteractionResult.FAIL;

        ItemStack held = player.getItemInHand(hand);

        // Sponge: anyone (owner or not) can clean one degradation level per sponge.
        if (held.getItem() instanceof SpongeEraserItem) {
            if (be.getDegradationLevel() == 0) {
                player.sendSystemMessage(Component.literal("§7§oCette affiche n'est pas dégradée."));
                return InteractionResult.CONSUME;
            }
            be.decrementDegradation();
            if (!player.getAbilities().instabuild) held.shrink(1);
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            com.vikingposter.network.NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(
                    () -> ((net.minecraft.server.level.ServerLevel) level).getChunkAt(pos)),
                new com.vikingposter.network.S2CSyncPosterPacket(pos, be.saveData()));
            player.sendSystemMessage(Component.literal(
                "§7§oÉponge appliquée — niveau de dégradation : §f"
                + be.getDegradationLevel() + "§7§o/" + PosterBlockEntity.MAX_DEGRADATION));
            return InteractionResult.CONSUME;
        }

        if (be.isOwner(player.getUUID())) {
            CompoundTag data = be.saveData();
            NetworkHandler.sendToClient(new S2COpenEditPacket(pos, data), serverPlayer);
        } else {
            if (be.isFullyDegraded()) {
                player.sendSystemMessage(Component.literal("§7§oCette affiche semble trop dégradée."));
                return InteractionResult.CONSUME;
            }
            long remaining = be.getPlayerCooldownRemaining(player.getUUID(), level.getGameTime());
            if (remaining > 0L) {
                long minutes = (remaining + 19L) / 20L / 60L; // ceil to minutes
                player.sendSystemMessage(Component.literal(
                    "§7§oTu as déjà sévi sur cette affiche récemment. Attends encore §f"
                    + minutes + " min§7§o."));
                return InteractionResult.CONSUME;
            }
            NetworkHandler.sendToClient(new S2COpenQTEPacket(pos, be.getDegradationLevel()), serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PosterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type != com.vikingposter.registration.ModBlockEntities.POSTER.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PosterBlockEntity>) (lvl, p, st, be) -> {
            if (!(lvl instanceof net.minecraft.server.level.ServerLevel sl)) return;
            if (!be.tickNaturalDegradation(sl.getGameTime())) return;
            be.setChanged();
            sl.sendBlockUpdated(p, st, st, Block.UPDATE_CLIENTS);
            com.vikingposter.network.NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> sl.getChunkAt(p)),
                new com.vikingposter.network.S2CSyncPosterPacket(p, be.saveData()));
        };
    }
}
