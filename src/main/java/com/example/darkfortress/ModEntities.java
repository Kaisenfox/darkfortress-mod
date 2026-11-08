package com.example.darkfortress;

import com.example.darkfortress.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static EntityType<HollowSoldierEntity> HOLLOW_SOLDIER;
    public static EntityType<HollowArcherEntity> HOLLOW_ARCHER;
    public static EntityType<BlackKnightEntity> BLACK_KNIGHT;
    public static EntityType<DemonLordEntity> DEMON_LORD;

    private ModEntities() {}

    private static <T extends Entity> EntityType<T> reg(String id, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(DarkFortressMod.MOD_ID, id), builder.build(id));
    }

    public static void init() {
        HOLLOW_SOLDIER = reg("hollow_soldier", EntityType.Builder.create(HollowSoldierEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.95f).maxTrackingRange(8));
        HOLLOW_ARCHER = reg("hollow_archer", EntityType.Builder.create(HollowArcherEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.99f).maxTrackingRange(8));
        BLACK_KNIGHT = reg("black_knight", EntityType.Builder.create(BlackKnightEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.72f, 2.35f).maxTrackingRange(10).makeFireImmune());
        DEMON_LORD = reg("demon_lord", EntityType.Builder.create(DemonLordEntity::new, SpawnGroup.MONSTER)
                .dimensions(1.35f, 4.3f).maxTrackingRange(10).makeFireImmune());

        FabricDefaultAttributeRegistry.register(HOLLOW_SOLDIER, HollowSoldierEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HOLLOW_ARCHER, HollowArcherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BLACK_KNIGHT, BlackKnightEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DEMON_LORD, DemonLordEntity.createAttributes());
    }
}
