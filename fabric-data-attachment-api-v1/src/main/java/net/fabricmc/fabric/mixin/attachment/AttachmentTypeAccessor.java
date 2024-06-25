package net.fabricmc.fabric.mixin.attachment;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AttachmentType.class)
public interface AttachmentTypeAccessor {
    @Accessor
    IAttachmentSerializer<?, ?> getSerializer();
}
