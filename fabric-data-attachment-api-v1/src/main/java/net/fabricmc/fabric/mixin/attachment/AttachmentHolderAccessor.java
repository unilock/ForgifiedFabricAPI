package net.fabricmc.fabric.mixin.attachment;

import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(AttachmentHolder.class)
public interface AttachmentHolderAccessor {
    @Invoker
    Map<AttachmentType<?>, Object> invokeGetAttachmentMap();
}
