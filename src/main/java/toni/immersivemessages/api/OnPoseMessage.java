package toni.immersivemessages.api;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;
import toni.lib.animation.AnimationKeyframe;
import toni.lib.animation.AnimationTimeline;

@FunctionalInterface
public interface OnPoseMessage {
    public abstract AnimationKeyframe applyPose(
        ImmersiveMessage tooltip,
        AnimationTimeline animation,
        GuiGraphics context,
        Vector2i bgOffset,
        TextAnchor anchor,
        TextAnchor align,
        float objectWidth,
        float objectHeight);
}
