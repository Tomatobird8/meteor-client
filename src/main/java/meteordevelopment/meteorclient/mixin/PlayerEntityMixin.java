/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.EntityMountWarning;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.utils.Utils.mc;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    protected void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        ClipAtLedgeEvent event = MeteorClient.EVENT_BUS.post(ClipAtLedgeEvent.get());

        if (event.isSet()) info.setReturnValue(event.isClip());
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void onDropItem(ItemStack stack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> info) {
        if (mc.world.isClient) {
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(stack)).isCancelled()) info.cancel();
        }
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At(value = "RETURN"), cancellable = true)
    public void onGetBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        SpeedMine module = Modules.get().get(SpeedMine.class);
        if (!module.isActive() || module.mode.get() != SpeedMine.Mode.Normal) return;

        cir.setReturnValue((float) (cir.getReturnValue() * module.modifier.get()));
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void dontJump(CallbackInfo info) {
        Anchor module = Modules.get().get(Anchor.class);
        if (module.isActive() && module.cancelJump) info.cancel();
    }
    
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    public void onInteractEntity(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    	EntityMountWarning warning = Modules.get().get(EntityMountWarning.class);
    	if (warning.isActive() && !mc.player.isOnGround() && !mc.player.isTouchingWater() && (entity instanceof HorseBaseEntity || entity instanceof LlamaEntity || entity instanceof PigEntity)) {
    		warning.warn();
    	}
    }
}
