package com.example.darkfortress;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarkFortressMod implements ModInitializer {
    public static final String MOD_ID = "darkfortress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ---- Tweakables ----
    /** Keep your items when you die (so you never lose the key or loot). */
    public static final boolean KEEP_INVENTORY = true;
    /** Stop natural monster spawns so only the fortress enemies exist. */
    public static final boolean DISABLE_MOB_SPAWNING = true;
    /** Freeze the world at dusk for a gloomier mood. */
    public static final boolean ETERNAL_DUSK = false;

    @Override
    public void onInitialize() {
        Scheduler.init();
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        registerCreativeTab();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GameRules rules = server.getGameRules();
            rules.get(GameRules.SPAWN_RADIUS).set(0, server);
            if (KEEP_INVENTORY) rules.get(GameRules.KEEP_INVENTORY).set(true, server);
            if (DISABLE_MOB_SPAWNING) rules.get(GameRules.DO_MOB_SPAWNING).set(false, server);
            if (ETERNAL_DUSK) {
                rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                server.getOverworld().setTimeOfDay(13000L);
            }
            Fortress.ensureBuilt(server.getOverworld());
        });

        // Starter kit and intro on first join.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (p.getCommandTags().contains("df_kit")) return;
            p.addCommandTag("df_kit");
            p.giveItemStack(new ItemStack(ModItems.ESTUS_FLASK));
            p.giveItemStack(new ItemStack(ModItems.BROKEN_SWORD));
            p.giveItemStack(new ItemStack(Items.SHIELD));
            ServerWorld w = server.getOverworld();
            BlockPos o = Fortress.origin(w);
            p.setSpawnPoint(w.getRegistryKey(), o.add(0, 0, 4), 0.0f, true, false);
            Fx.title(p, "FORTRESS OF THE FALLEN", Formatting.DARK_RED, "Rest at bonfires. Find the key. Slay the Demon.");
            p.sendMessage(Text.literal("You are Undead. Light bonfires (right-click) to rest and refill your Estus Flask.").formatted(Formatting.GOLD));
            p.sendMessage(Text.literal("Descend into the catacombs, defeat the Black Knight, take the key to the Keep. Hit suspicious walls.").formatted(Formatting.GRAY));
        });

        // YOU DIED
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                Fx.title(p, "YOU DIED", Formatting.DARK_RED, null);
            }
        });

        // Dying returns you to your last bonfire and resets the fortress.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (newPlayer.getServer() != null) {
                Fortress.respawnMobs(newPlayer.getServer().getOverworld());
            }
        });
    }

    private void registerCreativeTab() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.ESTUS_FLASK))
                .displayName(Text.translatable("itemGroup.darkfortress.main"))
                .entries((context, entries) -> {
                    entries.add(ModItems.ESTUS_FLASK);
                    entries.add(ModItems.BROKEN_SWORD);
                    entries.add(ModItems.BLACK_KNIGHT_GREATSWORD);
                    entries.add(ModItems.MOONLIGHT_GREATSWORD);
                    entries.add(ModItems.LIGHTNING_SPEAR);
                    entries.add(ModItems.PYROMANCY_FLAME);
                    entries.add(ModItems.DIVINE_BLESSING);
                    entries.add(ModItems.RING_OF_FAVOR);
                    entries.add(ModItems.BLACK_KNIGHT_HELM);
                    entries.add(ModItems.BLACK_KNIGHT_ARMOR);
                    entries.add(ModItems.BLACK_KNIGHT_LEGGINGS);
                    entries.add(ModItems.BLACK_KNIGHT_BOOTS);
                    entries.add(ModItems.SOUL_LOST_UNDEAD);
                    entries.add(ModItems.SOUL_GREAT_DEMON);
                    entries.add(ModItems.FORTRESS_KEY);
                    entries.add(ModBlocks.BONFIRE);
                    entries.add(ModBlocks.KEY_GATE);
                    entries.add(ModBlocks.ILLUSORY_WALL);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), group);
    }
}
