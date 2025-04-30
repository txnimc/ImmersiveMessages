package toni.immersivemessages.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import org.joml.Matrix4f;
import toni.lib.animation.AnimationKeyframe;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.render.CaxtonTextRenderer;

@FunctionalInterface
public interface OnRenderMessageCaxton {
    public abstract void render(ImmersiveMessage tooltip, GuiGraphics graphics, CaxtonTextRenderer renderer, CaxtonText text, int yOffset);
}