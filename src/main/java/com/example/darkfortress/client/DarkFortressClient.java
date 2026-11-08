package com.example.darkfortress.client;

import com.example.darkfortress.ModEntities;
import com.example.darkfortress.entity.DSMob;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class DarkFortressClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.<DSMob>register(ModEntities.HOLLOW_SOLDIER, ctx -> new HollowRenderer(ctx, "hollow_soldier", 1.0f));
        EntityRendererRegistry.<DSMob>register(ModEntities.BLACK_KNIGHT, ctx -> new HollowRenderer(ctx, "black_knight", 1.2f));
        EntityRendererRegistry.<DSMob>register(ModEntities.DEMON_LORD, ctx -> new HollowRenderer(ctx, "demon_lord", 2.2f));
        EntityRendererRegistry.register(ModEntities.HOLLOW_ARCHER, ArcherRenderer::new);
    }
}
