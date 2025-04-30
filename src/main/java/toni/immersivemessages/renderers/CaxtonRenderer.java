package toni.immersivemessages.renderers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FastColor;
import org.joml.Vector2i;
import org.joml.Vector3i;
import toni.immersivemessages.api.ImmersiveMessage;
import toni.immersivemessages.util.AnimationUtil;
import toni.immersivemessages.util.ImmersiveColor;
import toni.immersivemessages.util.RenderUtil;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.render.CaxtonTextRenderer;

import java.util.ArrayList;

public class CaxtonRenderer implements ITooltipRenderer {
    @Override
    public void render(ImmersiveMessage tooltip, GuiGraphics graphics, float deltaTicks) {
        var renderer = CaxtonTextRenderer.getInstance();
        var textLines = new ArrayList<FormattedText>();
        textLines.add(tooltip.getText());
        var size = wrapText(textLines, tooltip, renderer, tooltip.parent == null);
        var bgOffset = tooltip.anchor.getNormalized();
        bgOffset = bgOffset.add(tooltip.align.getNormalized().mul(-1));

        if (tooltip.background)
            RenderUtil.drawBackground(tooltip, graphics, size, bgOffset.mul(-3, new Vector2i()), deltaTicks);

        int yOffset = 0;
        for (int lineNumber = 0; lineNumber < textLines.size(); ++lineNumber)
        {
            FormattedText line = textLines.get(lineNumber);
            if (line != null)
            {
                CaxtonText text = CaxtonText.fromFormatted(
                    line,
                    renderer::getFontStorage,
                    tooltip.style,
                    false,
                    renderer.rtl,
                    renderer.getHandler().getCache());

                var lineWidth = renderer.getHandler().getWidth(text);
                lineWidth = adjustLineWidthForTypewriter(tooltip, graphics, lineWidth, textLines.size() == 1 ? tooltip.getRawText() : line, renderer);

                graphics.pose().pushPose();

                if (tooltip.onPoseMessage != null) {
                    tooltip.onPoseMessage.applyPose(tooltip, tooltip.animation, graphics, bgOffset.mul(-6, new Vector2i()), tooltip.anchor, tooltip.align, lineWidth, size.y);
                } else {
                    AnimationUtil.applyPose(tooltip.animation, graphics, bgOffset.mul(-6, new Vector2i()), tooltip.anchor, tooltip.align, lineWidth, size.y);
                    graphics.pose().translate(0.0D, 0.0D, 0.1f);
                }

                if (tooltip.onRenderMessageCaxton != null) {
                    tooltip.onRenderMessageCaxton.render(tooltip, graphics, renderer, text, yOffset);
                } else {
                    renderer.draw(text, 0, yOffset,
                        tooltip.animation.getColor(),
                        tooltip.shadow,
                        graphics.pose().last().pose(),
                        graphics.bufferSource(),
                        true,
                        0,
                        255,
                        0,
                        1000f);
                }

                graphics.pose().popPose();
            }

            yOffset += 10;
        }
    }

    private static float adjustLineWidthForTypewriter(ImmersiveMessage tooltip, GuiGraphics graphics, float lineWidth, FormattedText line, CaxtonTextRenderer renderer) {
        if (!tooltip.typewriter || tooltip.typewriterCenterAligned)
            return lineWidth;

        if (tooltip.wrapMaxWidth >= 0)
            return tooltip.wrapMaxWidth == 0 ? graphics.guiWidth() / 2f : Math.max(tooltip.wrapMaxWidth, graphics.guiWidth() / 2f);

        CaxtonText rawText = CaxtonText.fromFormatted(
            line,
            renderer::getFontStorage,
            tooltip.style,
            false,
            renderer.rtl,
            renderer.getHandler().getCache());

        return renderer.getHandler().getWidth(rawText);
    }

    private static Vector3i wrapText(ArrayList<FormattedText> textLines, ImmersiveMessage tooltip, CaxtonTextRenderer renderer, boolean isCallingFromRoot) {
        if (tooltip.parent == null || isCallingFromRoot) {
            var size = RenderUtil.wrapText(textLines, tooltip.wrapMaxWidth, (line) -> {
                CaxtonText txt = CaxtonText.fromFormatted(
                    line,
                    renderer::getFontStorage,
                    tooltip.style,
                    false,
                    renderer.rtl,
                    renderer.getHandler().getCache());

                return (int) renderer.getHandler().getWidth(txt);
            });

            if (tooltip.subtext != null) {
                var subtextLines = new ArrayList<FormattedText>();
                subtextLines.add(tooltip.subtext.getText());
                var subtextSize = wrapText(subtextLines, tooltip.subtext, renderer, true);

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
            return wrapText(textLines, tooltip.parent, renderer, false);
        }
    }
}
