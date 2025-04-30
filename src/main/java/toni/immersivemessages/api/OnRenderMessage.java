package toni.immersivemessages.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import org.joml.Matrix4f;
import toni.lib.animation.AnimationKeyframe;

@FunctionalInterface
public interface OnRenderMessage {
    public abstract void render(ImmersiveMessage tooltip, GuiGraphics graphics, FormattedText line, int yOffset);
}