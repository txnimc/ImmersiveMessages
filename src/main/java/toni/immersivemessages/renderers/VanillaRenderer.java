package toni.immersivemessages.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3i;
import toni.immersivemessages.api.ImmersiveMessage;
import toni.immersivemessages.util.AnimationUtil;
import toni.immersivemessages.util.RenderUtil;
import com.mojang.blaze3d.vertex.*;

import java.util.ArrayList;

public class VanillaRenderer implements ITooltipRenderer {
    @Override
    public void render(ImmersiveMessage tooltip, GuiGraphics graphics, float deltaTicks) {
        var font = Minecraft.getInstance().font;

        var textLines = new ArrayList<FormattedText>();
        textLines.add(tooltip.getText());

        var size = wrapText(textLines, tooltip, false);
        var bgOffset = tooltip.anchor.getNormalized();
        bgOffset = bgOffset.add(tooltip.align.getNormalized().mul(-1));

        if (tooltip.background)
            RenderUtil.drawBackground(tooltip, graphics, size, bgOffset.mul(-3, new Vector2i()), deltaTicks);

        int yOffset = 0;
        float fade = FastColor.ARGB32.alpha(tooltip.animation.getColor()) / 255f;
        MultiBufferSource.BufferSource renderType = graphics.bufferSource();

        for (int lineNumber = 0; lineNumber < textLines.size(); ++lineNumber)
        {
            FormattedText line = textLines.get(lineNumber);
            if (line != null)
            {
                graphics.pose().pushPose();

                var lineWidth = tooltip.typewriter && !tooltip.typewriterCenterAligned ? getTypewriterWidth(graphics, tooltip, textLines.size() == 1 ? tooltip.getRawText() : line) : font.width(line);

                if (tooltip.onPoseMessage != null) {
                    tooltip.onPoseMessage.applyPose(tooltip, tooltip.animation, graphics, bgOffset.mul(-6, new Vector2i()), tooltip.anchor, tooltip.align, lineWidth, size.y);
                } else {
                    AnimationUtil.applyPose(tooltip.animation, graphics, bgOffset.mul(-6, new Vector2i()), tooltip.anchor, tooltip.align, lineWidth, size.y);
                }


                if (tooltip.onRenderMessage != null) {
                    tooltip.onRenderMessage.render(tooltip, graphics, line, yOffset);
                } else {
                    font.drawInBatch(
                        Language.getInstance().getVisualOrder(line),
                        0,
                        yOffset,
                        FastColor.ARGB32.color((int) Math.max(0, Math.min(255, fade * 255)), 255, 255, 255),
                        tooltip.shadow,
                        graphics.pose().last().pose(),
                        renderType,
                        Font.DisplayMode.NORMAL,
                        0,
                        15728880);
                }

                graphics.pose().popPose();
            }

            yOffset += 10;
        }

        graphics.flush();
    }

    private static Vector3i wrapText(ArrayList<FormattedText> textLines, ImmersiveMessage tooltip, boolean isCallingFromRoot) {
        if (tooltip.parent == null || isCallingFromRoot) {
            var size = RenderUtil.wrapText(textLines, tooltip.wrapMaxWidth, (line) -> Minecraft.getInstance().font.width(line));

            if (tooltip.subtext != null) {
                var subtextLines = new ArrayList<FormattedText>();
                subtextLines.add(tooltip.subtext.getText());
                var subtextSize = wrapText(subtextLines, tooltip.subtext, true);

                var yOffset = Math.max(0, tooltip.subtext.yLevel - tooltip.yLevel);
                var xOffset = Math.max(0, tooltip.subtext.xLevel - tooltip.xLevel);
                return new Vector3i(
                    Math.max(size.x, (int) xOffset + subtextSize.x),
                    Math.max(size.y, (int) yOffset + subtextSize.y),
                    Math.max(size.z, subtextSize.z)
                );
            }

            return size;
        } else {
            return wrapText(textLines, tooltip.parent, false);
        }
    }

    private float getTypewriterWidth(GuiGraphics graphics, ImmersiveMessage tooltip, FormattedText line) {
        if (tooltip.wrapMaxWidth >= 0)
            return tooltip.wrapMaxWidth == 0 ? graphics.guiWidth() / 2f : Math.max(tooltip.wrapMaxWidth, graphics.guiWidth() / 2f);

        return Minecraft.getInstance().font.width(line);
    }
}
