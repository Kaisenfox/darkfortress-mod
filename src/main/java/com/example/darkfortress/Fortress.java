package com.example.darkfortress;

import com.example.darkfortress.entity.DSMob;
import com.example.darkfortress.entity.DemonLordEntity;
import com.example.darkfortress.entity.HollowArcherEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Generates and manages the Dark Fortress at world spawn.
 *
 * Layout (x east, z south, y relative to the courtyard floor; origin = world spawn):
 *  - Courtyard (hub): bonfire at (0,0,3), start of the game.
 *  - Curtain wall with ramparts, 4 corner towers (NW tower hides an illusory-wall secret).
 *  - Catacombs below the courtyard: Black Knight hall (drops the key), 2nd bonfire, secret alcove.
 *  - Keep to the north: locked gate, great hall + 3rd bonfire, stairs up to the boss arena.
 */
public final class Fortress {
    public static final int FLAG_BUILT = 0;
    public static final int FLAG_KNIGHT = 1;
    public static final int FLAG_BOSS = 2;

    private static final String ADD_TAG = "df_boss_add";
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final int NO_SUPPORT = Integer.MIN_VALUE;

    // type (0 = soldier, 1 = archer), x, y, z  (relative to origin)
    private static final int[][] SPAWNS = {
            // courtyard
            {0, -18, 0, 10}, {0, 18, 0, 12}, {0, -6, 0, -9}, {0, 8, 0, -10}, {0, -22, 0, 22}, {0, 24, 0, 24},
            // ramparts
            {1, -34, 9, -10}, {1, -34, 9, 12}, {1, 34, 9, -8}, {1, 34, 9, 14},
            // towers
            {0, 32, 9, 32}, {0, -32, 9, 32}, {0, 32, 9, -32}, {0, -32, 9, -30},
            // catacombs
            {0, -10, -9, 3}, {0, 10, -9, 3}, {0, -16, -9, 3}, {1, 18, -9, 3}, {0, -6, -9, 10}, {0, 6, -9, 20},
            // keep great hall
            {0, -8, 0, -20}, {0, 8, 0, -26}, {0, -8, 0, -28}, {0, 0, 0, -24}, {1, -12, 0, -18}, {1, 12, 0, -29},
    };

    private Fortress() {}

    // ------------------------------------------------------------------ origin / flags

    public static BlockPos origin(ServerWorld world) {
        BlockPos sp = world.getSpawnPos();
        int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, sp.getX(), sp.getZ());
        int fy = Math.max(top, world.getSeaLevel() + 1);
        return new BlockPos(sp.getX(), fy, sp.getZ());
    }

    private static BlockPos flagPos(ServerWorld world, int idx) {
        BlockPos o = origin(world);
        return new BlockPos(o.getX() - 30 + idx, o.getY() - 8, o.getZ() - 30);
    }

    public static boolean hasFlag(ServerWorld world, int idx) {
        return world.getBlockState(flagPos(world, idx)).isOf(Blocks.RED_NETHER_BRICKS);
    }

    public static void setFlag(ServerWorld world, int idx) {
        world.setBlockState(flagPos(world, idx), Blocks.RED_NETHER_BRICKS.getDefaultState(), 2);
    }

    // ------------------------------------------------------------------ public API

    public static void ensureBuilt(ServerWorld world) {
        if (hasFlag(world, FLAG_BUILT)) return;
        DarkFortressMod.LOGGER.info("Building the Dark Fortress at spawn (first run, this takes a moment)...");
        build(world);
        setFlag(world, FLAG_BUILT);
        respawnMobs(world);
        DarkFortressMod.LOGGER.info("Dark Fortress built.");
    }

    public static void sealArena(ServerWorld world) {
        BlockPos o = origin(world);
        for (int x = 12; x <= 13; x++)
            for (int y = 9; y <= 11; y++)
                for (int z = -25; z <= -22; z++)
                    world.setBlockState(o.add(x, y, z), Blocks.IRON_BARS.getDefaultState(), 2);
    }

    public static void openArena(ServerWorld world) {
        BlockPos o = origin(world);
        for (int x = 12; x <= 13; x++)
            for (int y = 9; y <= 11; y++)
                for (int z = -25; z <= -22; z++)
                    if (world.getBlockState(o.add(x, y, z)).isOf(Blocks.IRON_BARS))
                        world.setBlockState(o.add(x, y, z), AIR, 2);
    }

    public static void respawnMobs(ServerWorld world) {
        BlockPos o = origin(world);
        Box box = new Box(o.getX() - 40, o.getY() - 45, o.getZ() - 40, o.getX() + 40, o.getY() + 40, o.getZ() + 40);

        for (MobEntity m : world.getEntitiesByClass(MobEntity.class, box,
                e -> (e instanceof DSMob && !(e instanceof DemonLordEntity)) || e instanceof HollowArcherEntity)) {
            m.discard();
        }

        if (!hasFlag(world, FLAG_BOSS)) {
            for (DemonLordEntity d : world.getEntitiesByClass(DemonLordEntity.class, box, e -> true)) {
                d.clearBar();
                d.discard();
            }
            openArena(world);
            spawnMob(world, ModEntities.DEMON_LORD, o.getX() + 0.5, o.getY() + 9, o.getZ() - 23.5,
                    new ItemStack(ModItems.BLACK_KNIGHT_GREATSWORD));
        }
        if (!hasFlag(world, FLAG_KNIGHT)) {
            spawnMob(world, ModEntities.BLACK_KNIGHT, o.getX() + 0.5, o.getY() - 9, o.getZ() + 15.5,
                    new ItemStack(ModItems.BLACK_KNIGHT_GREATSWORD));
        }
        for (int[] s : SPAWNS) {
            double x = o.getX() + s[1] + 0.5;
            double y = o.getY() + s[2];
            double z = o.getZ() + s[3] + 0.5;
            if (s[0] == 0) {
                spawnMob(world, ModEntities.HOLLOW_SOLDIER, x, y, z, new ItemStack(Items.IRON_SWORD));
            } else {
                spawnMob(world, ModEntities.HOLLOW_ARCHER, x, y, z, new ItemStack(Items.BOW));
            }
        }
    }

    public static void spawnAdd(ServerWorld world, double x, double y, double z) {
        MobEntity m = spawnMob(world, ModEntities.HOLLOW_SOLDIER, x, y, z, new ItemStack(Items.IRON_SWORD));
        if (m != null) m.addCommandTag(ADD_TAG);
    }

    public static void onBossDefeated(ServerWorld world, DemonLordEntity boss) {
        setFlag(world, FLAG_BOSS);
        openArena(world);

        BlockPos o = origin(world);
        BlockPos bp = o.add(0, 9, -21);
        world.setBlockState(bp, ModBlocks.BONFIRE.getDefaultState(), 3);
        world.setBlockState(bp.up(), Blocks.END_ROD.getDefaultState(), 3);

        for (MobEntity m : world.getEntitiesByClass(MobEntity.class, boss.getBoundingBox().expand(60),
                e -> e.getCommandTags().contains(ADD_TAG))) {
            m.discard();
        }

        boss.dropStack(new ItemStack(ModItems.MOONLIGHT_GREATSWORD));
        boss.dropStack(new ItemStack(ModItems.SOUL_GREAT_DEMON));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_HELM));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_ARMOR));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_LEGGINGS));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_BOOTS));

        for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(boss) < 4096.0)) {
            Fx.title(p, "DEMON SLAIN", Formatting.GOLD, "A bonfire has been lit");
        }
    }

    private static MobEntity spawnMob(ServerWorld world, EntityType<? extends MobEntity> type,
                                      double x, double y, double z, ItemStack weapon) {
        MobEntity mob = type.create(world);
        if (mob == null) return null;
        mob.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0f, 0.0f);
        if (!weapon.isEmpty()) {
            mob.equipStack(EquipmentSlot.MAINHAND, weapon);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        }
        mob.setPersistent();
        world.spawnEntity(mob);
        return mob;
    }

    // ------------------------------------------------------------------ builder helper

    private static final class B {
        final ServerWorld w;
        final int ox, oy, oz;
        final Random rnd = new Random(20260607L);
        final BlockPos.Mutable m = new BlockPos.Mutable();

        B(ServerWorld w, int ox, int oy, int oz) {
            this.w = w;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
        }

        BlockState get(int x, int y, int z) {
            return w.getBlockState(m.set(ox + x, oy + y, oz + z));
        }

        void set(int x, int y, int z, BlockState s) {
            w.setBlockState(m.set(ox + x, oy + y, oz + z), s, 2);
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
            fillWith(x1, y1, z1, x2, y2, z2, () -> s);
        }

        void fillWith(int x1, int y1, int z1, int x2, int y2, int z2, Supplier<BlockState> s) {
            int ax = Math.min(x1, x2), bx = Math.max(x1, x2);
            int ay = Math.min(y1, y2), by = Math.max(y1, y2);
            int az = Math.min(z1, z2), bz = Math.max(z1, z2);
            for (int x = ax; x <= bx; x++)
                for (int y = ay; y <= by; y++)
                    for (int z = az; z <= bz; z++)
                        set(x, y, z, s.get());
        }

        BlockState brick() {
            int r = rnd.nextInt(100);
            if (r < 62) return Blocks.STONE_BRICKS.getDefaultState();
            if (r < 80) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            if (r < 95) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
            return Blocks.COBBLESTONE.getDefaultState();
        }

        BlockState dark() {
            int r = rnd.nextInt(100);
            if (r < 55) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
            if (r < 80) return Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
            if (r < 92) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            return Blocks.BLACKSTONE.getDefaultState();
        }

        BlockState pave() {
            int r = rnd.nextInt(100);
            if (r < 55) return Blocks.STONE_BRICKS.getDefaultState();
            if (r < 75) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            if (r < 90) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
            if (r < 96) return Blocks.COBBLESTONE.getDefaultState();
            return Blocks.COARSE_DIRT.getDefaultState();
        }

        void stairs(int x1, int x2, int y0, int z0, int dy, int dz, int steps, Direction facing,
                    Block stair, int supportFrom, Supplier<BlockState> support) {
            BlockState st = stair.getDefaultState().with(StairsBlock.FACING, facing);
            for (int i = 0; i < steps; i++) {
                int y = y0 + dy * i;
                int z = z0 + dz * i;
                fill(x1, y + 1, z, x2, y + 3, z, AIR);
                if (supportFrom != NO_SUPPORT && y - 1 >= supportFrom) {
                    fillWith(x1, supportFrom, z, x2, y - 1, z, support);
                }
                fill(x1, y, z, x2, y, z, st);
            }
        }

        void lantern(int x, int y, int z, boolean hanging) {
            BlockState s = Blocks.SOUL_LANTERN.getDefaultState();
            if (hanging) s = s.with(LanternBlock.HANGING, true);
            set(x, y, z, s);
        }

        void bonfire(int x, int y, int z) {
            set(x, y, z, ModBlocks.BONFIRE.getDefaultState());
            set(x, y + 1, z, Blocks.END_ROD.getDefaultState());
        }

        void chest(int x, int y, int z, ItemStack... stacks) {
            set(x, y, z, Blocks.CHEST.getDefaultState());
            BlockEntity be = w.getBlockEntity(new BlockPos(ox + x, oy + y, oz + z));
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < stacks.length; i++) chest.setStack(i, stacks[i]);
            }
        }
    }

    // ------------------------------------------------------------------ the build

    private static void build(ServerWorld world) {
        BlockPos o = origin(world);
        B b = new B(world, o.getX(), o.getY(), o.getZ());
        BlockState stone = Blocks.STONE.getDefaultState();

        // 1. Clear everything in the footprint (trees, hills).
        b.fill(-37, 0, -37, 37, 32, 37, AIR);

        // 2. Foundation + skirt so the fortress never floats.
        b.fill(-36, -14, -36, 36, -2, 36, stone);
        for (int x = -36; x <= 36; x++) {
            for (int z = -36; z <= 36; z++) {
                if (Math.abs(x) < 33 && Math.abs(z) < 33) continue;
                for (int y = -15; y >= -40; y--) {
                    BlockState s = b.get(x, y, z);
                    if (!(s.isAir() || !s.getFluidState().isEmpty())) break;
                    b.set(x, y, z, stone);
                }
            }
        }

        // 3. Courtyard paving.
        b.fillWith(-33, -1, -33, 33, -1, 33, b::pave);

        // 4. Curtain wall.
        b.fillWith(-36, -1, -36, 36, 8, -34, b::brick);
        b.fillWith(-36, -1, 34, 36, 8, 36, b::brick);
        b.fillWith(-36, -1, -33, -34, 8, 33, b::brick);
        b.fillWith(34, -1, -33, 36, 8, 33, b::brick);
        for (int i = -36; i <= 36; i += 2) {
            for (int y = 9; y <= 10; y++) {
                b.set(i, y, -36, b.brick());
                b.set(i, y, 36, b.brick());
                b.set(-36, y, i, b.brick());
                b.set(36, y, i, b.brick());
            }
        }
        // South gate arch with a half-raised portcullis.
        b.fill(-4, 0, 34, 4, 6, 36, AIR);
        b.fill(-4, 5, 35, 4, 6, 35, Blocks.IRON_BARS.getDefaultState());

        // 5. Corner towers.
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int xa = sx * 28, xb = sx * 36, za = sz * 28, zb = sz * 36;
                b.fillWith(xa, -1, za, xb, 15, zb, b::brick);
                b.fill(sx * 29, 9, sz * 29, sx * 35, 14, sz * 35, AIR);
                int minX = Math.min(xa, xb), maxX = Math.max(xa, xb);
                int minZ = Math.min(za, zb), maxZ = Math.max(za, zb);
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                        if (edge && Math.floorMod(x + z, 2) == 0) b.set(x, 16, z, b.brick());
                    }
                }
                if (!(sx == -1 && sz == -1)) {
                    b.fill(sx * 28, 9, sz * 35, sx * 28, 10, sz * 35, AIR);
                }
                b.fill(sx * 35, 9, sz * 28, sx * 35, 10, sz * 28, AIR);
                b.lantern(sx * 35, 9, sz * 35, false);
            }
        }
        // NW tower secret: an illusory wall splits the tower; chamber behind holds a chest.
        b.fill(-35, 9, -32, -29, 14, -32, ModBlocks.ILLUSORY_WALL.getDefaultState());
        b.set(-35, 9, -35, AIR);
        b.lantern(-33, 9, -34, false);
        b.chest(-32, 9, -35,
                new ItemStack(ModItems.RING_OF_FAVOR),
                new ItemStack(ModItems.PYROMANCY_FLAME),
                new ItemStack(ModItems.SOUL_LOST_UNDEAD, 6));
        // SE tower: an easy chest of souls.
        b.chest(34, 9, 34, new ItemStack(ModItems.SOUL_LOST_UNDEAD, 8));

        // 6. Rampart stairs (both sides).
        for (int side : new int[]{-1, 1}) {
            int x1 = side == -1 ? -33 : 32;
            int x2 = side == -1 ? -32 : 33;
            b.stairs(x1, x2, 0, 8, 1, 1, 9, Direction.SOUTH, Blocks.STONE_BRICK_STAIRS, -1, b::brick);
        }

        // 7. The Keep.
        b.fillWith(-15, -1, -32, 15, 24, -14, b::dark);
        b.fill(-13, 0, -30, 13, 7, -16, AIR);     // great hall
        b.fill(-13, 9, -30, 13, 21, -16, AIR);    // boss arena
        for (int x = -15; x <= 15; x += 2) {
            b.set(x, 25, -32, b.dark());
            b.set(x, 25, -14, b.dark());
        }
        for (int z = -32; z <= -14; z += 2) {
            b.set(-15, 25, z, b.dark());
            b.set(15, 25, z, b.dark());
        }
        // Locked gate.
        b.fill(-2, 0, -15, 2, 4, -14, ModBlocks.KEY_GATE.getDefaultState());
        // Hall pillars and hanging lanterns.
        for (int px : new int[]{-8, 8}) {
            for (int pz : new int[]{-19, -23, -27}) {
                b.fillWith(px, 0, pz, px, 7, pz, b::dark);
            }
        }
        for (int lx : new int[]{-4, 4}) {
            for (int lz : new int[]{-18, -24, -28}) {
                b.lantern(lx, 7, lz, true);
            }
        }
        b.bonfire(0, 0, -18);
        // Stairs up to the arena (steps cut through the arena floor).
        b.stairs(12, 13, 0, -17, 1, -1, 9, Direction.NORTH, Blocks.DEEPSLATE_BRICK_STAIRS, 0, b::dark);
        // Arena dressing.
        for (int px : new int[]{-9, 9}) {
            for (int pz : new int[]{-20, -26}) {
                b.fillWith(px, 9, pz, px, 20, pz, b::dark);
            }
        }
        for (int lx : new int[]{-5, 5}) {
            for (int lz : new int[]{-20, -28}) {
                b.lantern(lx, 21, lz, true);
            }
        }
        b.set(-6, 9, -23, Blocks.SOUL_CAMPFIRE.getDefaultState());
        b.set(6, 9, -23, Blocks.SOUL_CAMPFIRE.getDefaultState());

        // 8. Catacombs.
        b.fill(-23, -9, 1, -19, -5, 5, AIR);      // landing
        b.fill(-23, -9, 2, 24, -5, 4, AIR);       // main corridor
        b.fill(-9, -9, 6, 9, -3, 24, AIR);        // Black Knight hall
        b.fill(-2, -9, 5, 2, -6, 5, AIR);         // hall doorway
        b.fill(24, -9, 0, 30, -5, 7, AIR);        // bonfire room
        b.fill(-14, -9, 6, -11, -6, 10, AIR);     // secret alcove
        b.fill(-12, -9, 5, -12, -6, 5, ModBlocks.ILLUSORY_WALL.getDefaultState());
        // Entrance stairs from the courtyard (descend south) and exit stairs (ascend north).
        b.stairs(-22, -20, -1, -9, -1, 1, 10, Direction.NORTH, Blocks.STONE_BRICK_STAIRS, NO_SUPPORT, b::brick);
        b.stairs(26, 28, -10, -1, 1, -1, 10, Direction.NORTH, Blocks.STONE_BRICK_STAIRS, NO_SUPPORT, b::brick);
        // Dressing.
        for (int px : new int[]{-5, 5}) {
            for (int pz : new int[]{10, 18}) {
                b.fillWith(px, -9, pz, px, -3, pz, b::dark);
            }
        }
        for (int lx = -16; lx <= 16; lx += 8) b.lantern(lx, -5, 3, true);
        for (int lx : new int[]{-7, 7}) {
            for (int lz : new int[]{8, 16, 22}) b.lantern(lx, -3, lz, true);
        }
        b.lantern(-21, -5, 3, true);
        b.lantern(27, -5, 5, true);
        b.bonfire(27, -9, 4);
        b.lantern(-12, -6, 8, true);
        b.chest(-12, -9, 9,
                new ItemStack(ModItems.LIGHTNING_SPEAR),
                new ItemStack(ModItems.DIVINE_BLESSING, 2),
                new ItemStack(ModItems.SOUL_LOST_UNDEAD, 6));
        for (int i = 0; i < 50; i++) {
            int x = -22 + b.rnd.nextInt(45);
            int z = 2 + b.rnd.nextInt(3);
            if (b.get(x, -5, z).isAir()) b.set(x, -5, z, Blocks.COBWEB.getDefaultState());
        }

        // 9. Courtyard dressing (never touch the spawn column x=0,z=0).
        int[][] pillars = {{-12, 6}, {12, 6}, {-12, -6}, {12, -6}, {-24, -10}, {24, -10}, {-24, 20}, {24, 20}};
        for (int[] p : pillars) {
            int h = 3 + b.rnd.nextInt(5);
            b.fillWith(p[0], 0, p[1], p[0], h, p[1], b::brick);
        }
        for (int i = 0; i < 70; i++) {
            int x = -32 + b.rnd.nextInt(65);
            int z = -12 + b.rnd.nextInt(44);
            if (Math.abs(x) <= 4 && z >= -4 && z <= 8) continue;
            if (x >= -23 && x <= -19 && z >= -9 && z <= 0) continue;
            if (x >= 25 && x <= 29 && z >= -11 && z <= -1) continue;
            if (Math.abs(x) >= 31 && z >= 7 && z <= 17) continue;
            if (b.get(x, 0, z).isAir()) {
                b.set(x, 0, z, b.rnd.nextBoolean()
                        ? Blocks.COBBLESTONE_SLAB.getDefaultState()
                        : Blocks.MOSSY_COBBLESTONE.getDefaultState());
            }
        }
        b.set(-5, 0, 6, Blocks.SOUL_CAMPFIRE.getDefaultState());
        b.set(5, 0, 6, Blocks.SOUL_CAMPFIRE.getDefaultState());
        int[][] lanterns = {{-32, -8}, {-32, -24}, {-32, 28}, {32, -8}, {32, -24}, {32, 28},
                {-24, 32}, {-12, 32}, {12, 32}, {24, 32}};
        for (int[] l : lanterns) b.lantern(l[0], 0, l[1], false);

        // 10. The starting bonfire.
        b.bonfire(0, 0, 3);

        // 11. Hidden stone marker row under the floor must not exist yet (flags set by caller).
    }
}
