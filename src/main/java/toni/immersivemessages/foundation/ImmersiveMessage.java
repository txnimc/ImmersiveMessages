package toni.immersivemessages.foundation;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import toni.immersivemessages.ImmersiveMessages;
import toni.immersivemessages.foundation.networking.TooltipPacket;
import toni.lib.animation.AnimationTimeline;
import toni.lib.animation.Binding;
import toni.lib.animation.easing.EasingType;
import toni.lib.utils.VersionUtils;

#if MC > "201"
import net.minecraft.network.codec.StreamCodec;
#else
import toni.lib.networking.codecs.StreamCodec;
#endif


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

public class ImmersiveMessage {
    public Style style = Style.EMPTY;
    public String text;
    public AnimationTimeline animation;
    public ImmersiveMessage subtext;
    public boolean shadow = true;
    public boolean border;

    public float yLevel = 55f;
    public float xLevel = 0f;
    public float delay = 0f;

    private ObfuscateMode obfuscateMode = ObfuscateMode.NONE;
    private float obfuscateSpeed = 1f;
    private float obfuscateTicks;
    private int obfuscateTimes = 1;

    private ImmersiveMessage() { }


    public static final StreamCodec<ByteBuf, ImmersiveMessage> CODEC = new StreamCodec<>() {
        public ImmersiveMessage decode(ByteBuf byteBuf) {
            return ImmersiveMessage.decode(byteBuf);
        }

        public void encode(ByteBuf byteBuf, ImmersiveMessage tooltip) {
            tooltip.encode(byteBuf);
        }
    };

    private void encode(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);

        #if MC > "201"
        buf.writeJsonWithCodec(Style.Serializer.CODEC, style);
        #else
        buf.writeJsonWithCodec(Style.FORMATTING_CODEC, style);
        #endif

        buf.writeUtf(text);
        animation.encode(buf);

        buf.writeBoolean(subtext != null);
        if (subtext != null) {
            subtext.encode(buf);
        }

        buf.writeBoolean(shadow);
        buf.writeBoolean(border);

        buf.writeFloat(yLevel);
        buf.writeFloat(xLevel);
        buf.writeFloat(delay);

        buf.writeEnum(obfuscateMode);
        buf.writeFloat(obfuscateSpeed);
        buf.writeFloat(obfuscateTicks);
        buf.writeInt(obfuscateTimes);
    }


    private static ImmersiveMessage decode(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var ths = new ImmersiveMessage();

        #if MC > "201"
        ths.style = buf.readJsonWithCodec(Style.Serializer.CODEC);
        #else
        ths.style = buf.readJsonWithCodec(Style.FORMATTING_CODEC);
        #endif

        ths.text = buf.readUtf();
        ths.animation = AnimationTimeline.decode(buf);

        var hasSubtext = buf.readBoolean();
        if (hasSubtext) {
            ths.subtext = ImmersiveMessage.decode(buf);
        }

        ths.shadow = buf.readBoolean();
        ths.border = buf.readBoolean();
        ths.yLevel = buf.readFloat();
        ths.xLevel = buf.readFloat();
        ths.delay = buf.readFloat();

        ths.obfuscateMode = buf.readEnum(ObfuscateMode.class);
        ths.obfuscateSpeed = buf.readFloat();
        ths.obfuscateTicks = buf.readFloat();
        ths.obfuscateTimes = buf.readInt();

        return ths;
    }


    /**
     * Creates a new Immersive Tooltip with the specified text and duration.
     * @param duration how long the animation should play for
     * @param text the string to show
     * @return the tooltip builder
     */
    public static ImmersiveMessage builder(float duration, String text) {
        ImmersiveMessage tooltip = new ImmersiveMessage();
        tooltip.text = text;
        tooltip.style = Style.EMPTY;
        tooltip.animation = AnimationTimeline.builder(duration);
        tooltip.animation.withYPosition(tooltip.yLevel);
        return tooltip;
    }

    /**
     * Allows for custom consumer configuration methods
     * @return the tooltip builder
     */
    public ImmersiveMessage apply(Consumer<ImmersiveMessage> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * Allows recursive chaining of messages in the same configuration, mostly useful for subtext.
     * @param delay the starting delay. By default, the subtext will run for the rest of the animation
     * @param subtext the text to display
     * @param builder consumer method to configure the subtext
     * @return the tooltip builder
     */
    public ImmersiveMessage subtext(float delay, String subtext, Consumer<ImmersiveMessage> builder) {
        return subtext(delay, subtext, 10f, builder);
    }

    /**
     * Allows recursive chaining of messages in the same configuration, mostly useful for subtext.
     * @param delay the starting delay. By default, the subtext will run for the rest of the animation
     * @param subtext the text to display
     * @param offset the Y offset of the subtext, default 10f
     * @param builder consumer method to configure the subtext
     * @return the tooltip builder
     */
    public ImmersiveMessage subtext(float delay, String subtext, float offset, Consumer<ImmersiveMessage> builder) {
        this.subtext = ImmersiveMessage.builder(animation.duration, subtext);
        this.subtext.delay = delay;
        this.subtext.y(this.yLevel + offset);
        this.subtext.size(0.75f);
        builder.accept(this.subtext);
        return this;
    }

    /**
     * Sets the initial Y level
     * @param ylevel the starting level
     * @return the tooltip builder
     */
    public ImmersiveMessage y(float ylevel) {
        this.yLevel = ylevel;
        animation.withYPosition(ylevel);
        return this;
    }

    /**
     * Sets the initial X level
     * @param xlevel the starting level
     * @return the tooltip builder
     */
    public ImmersiveMessage x(float xlevel) {
        this.xLevel = xlevel;
        animation.withXPosition(xlevel);
        return this;
    }

    public ImmersiveMessage bold() {
        return style(style -> style.withBold(true));
    }

    public ImmersiveMessage italic() {
        return style(style -> style.withItalic(true));
    }

    /**
     * The starting scale of the font. Please note that animations using the scale binding may override this!
     * @param size scale, default 1f
     * @return the tooltip builder
     */
    public ImmersiveMessage size(float size) {
        animation.withSize(size);
        return this;
    }

    public ImmersiveMessage fadeIn() { return fadeIn(1f); }
    public ImmersiveMessage fadeIn(float duration) {
        animation.transition(Binding.Alpha, delay, delay + duration, 0f, 1f, EasingType.EaseOutSine);
        return this;
    }

    public ImmersiveMessage fadeOut() { return fadeOut(1f); }
    public ImmersiveMessage fadeOut(float duration) {
        animation.fadeout(duration);
        return this;
    }

    /**
        Violently shakes the text in the X and Y axis.
     */
    public ImmersiveMessage shake() { return shake(100f, 0.75f); }

    /**
     * Violently shakes the text in the X and Y axis.
     * @param speed default 100f
     * @param intensity default 0.75f
     * @return the tooltip builder
     */
    public ImmersiveMessage shake(float speed, float intensity) {
        animation.waveEffect(Binding.xPos, intensity, speed, 0f, animation.duration);
        animation.waveEffect(Binding.yPos, intensity, speed * 0.95f, 0f, animation.duration);
        return this;
    }


    /**
     * Mild sine waving effect on the Z rotation.
     * @return the tooltip builder
     */
    public ImmersiveMessage wave() { return wave(5f, 2.5f); }

    /**
     * Mild sine waving effect on the Z rotation.
     * @param speed how fast the sine wave goes
     * @param intensity how much movement
     * @return the tooltip builder
     */
    public ImmersiveMessage wave(float speed, float intensity) {
        animation.waveEffect(Binding.zRot, intensity, speed, 0f, animation.duration);
        return this;
    }

    public ImmersiveMessage slideUp() { return slideUp(1f); }
    public ImmersiveMessage slideUp(float duration) {
        animation.transition(Binding.yPos, delay, delay + duration, yLevel + 50f, yLevel, EasingType.EaseOutCubic);
        return this;
    }

    public ImmersiveMessage slideDown() { return slideDown(1f); }
    public ImmersiveMessage slideDown(float duration) {
        animation.transition(Binding.yPos, delay, delay + duration, yLevel - 50f, yLevel, EasingType.EaseOutCubic);
        return this;
    }

    public ImmersiveMessage slideLeft() { return slideLeft(1f); }
    public ImmersiveMessage slideLeft(float duration) {
        animation.transition(Binding.xPos, delay, delay + duration, xLevel - 50f, xLevel, EasingType.EaseOutCubic);
        return this;
    }

    public ImmersiveMessage slideRight() { return slideRight(1f); }
    public ImmersiveMessage slideRight(float duration) {
        animation.transition(Binding.xPos, delay, delay + duration, xLevel + 50f, xLevel, EasingType.EaseOutCubic);
        return this;
    }

    /**
     * Sets a custom font. Please note that non-vanilla fonts will attempt to use Caxton rendering, which
     * you will need installed to work properly!
     * @param font the custom font
     * @return the tooltip builder
     */
    public ImmersiveMessage font(String font) {
        this.style = style.withFont(VersionUtils.resource(font));
        return this;
    }

    /**
     * Sets a custom font. Please note that non-vanilla fonts will attempt to use Caxton rendering, which
     * you will need installed to work properly!
     * @param font the custom font
     * @return the tooltip builder
     */
    public ImmersiveMessage font(ImmersiveFont font) {
        this.style = style.withFont(font.getLocation());
        return this;
    }

    /**
     * Applies a new style, overwriting the old one.
     * @param style the new style to apply
     * @return the tooltip builder
     */
    public ImmersiveMessage style(Style style) {
        this.style = style;
        return this;
    }

    /**
     * Modifies the current style with a function.
     * @param function
     * @return the tooltip builder
     */
    public ImmersiveMessage style(Function<Style, Style> function) {
        this.style = function.apply(style);
        return this;
    }

    public ImmersiveMessage shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /**
     * Provides access to the {@link AnimationTimeline} for custom animations
     * @param animationBuilder a consumer that modifies the timeline
     * @return the tooltip builder
     */
    public ImmersiveMessage animation(Consumer<AnimationTimeline> animationBuilder) {
        animationBuilder.accept(this.animation);
        return this;
    }

    public void sendLocal(LocalPlayer player) {
        ImmersiveMessages.showToPlayer(player, this);
    }

    public void sendServer(ServerPlayer player) {
        new TooltipPacket(this).send(player);
    }


    public void sendServer(Collection<ServerPlayer> players) {
        players.forEach(this::sendServer);
    }

    public void sendServerToAll(MinecraftServer server) {
        new TooltipPacket(this).sendToAll(server);
    }

    /**
     * Randomly fades in the text with obfuscation. Supports custom fonts!
     * @return the tooltip builder
     */
    public ImmersiveMessage obfuscate() {
        return this.obfuscate(ObfuscateMode.RANDOM, 1f);
    }

    /**
     * Randomly fades in the text with obfuscation. Supports custom fonts!
     * @param speed how fast the text should clear, default 1f
     * @return the tooltip builder
     */
    public ImmersiveMessage obfuscate(float speed) {
        return this.obfuscate(ObfuscateMode.RANDOM, speed);
    }

    /**
     * Fades in the text with obfuscation using a custom mode. Supports custom fonts!
     * @param mode the direction/method the text will fade in with
     * @return the tooltip builder
     */
    public ImmersiveMessage obfuscate(ObfuscateMode mode) {
        return this.obfuscate(mode, 1f);
    }

    /**
     * Fades in the text with obfuscation using a custom mode. Supports custom fonts!
     * @param mode the direction/method the text will fade in with
     * @param speed how fast the text should clear, default 1f
     * @return the tooltip builder
     */
    public ImmersiveMessage obfuscate(ObfuscateMode mode, float speed) {
        this.obfuscateMode = mode;
        this.obfuscateSpeed = speed;

        var sb = new StringBuilder();
        for (var chr : text.toCharArray()) {
            sb.append("§k");
            sb.append(chr);
            sb.append("§r");
        }

        text = sb.toString();
        return this;
    }

    public ImmersiveMessage color(@Nullable TextColor color) {
         return this.style(style -> style.withColor(color));
    }

    public ImmersiveMessage color(@Nullable ChatFormatting formatting) {
        return this.style(style -> style.withColor(formatting));
    }

    public ImmersiveMessage color(int rgb) {
        return this.style(style -> style.withColor(rgb));
    }

    public void tickObfuscation(float delta) {
        obfuscateTicks += delta;
        if (!(obfuscateTicks > obfuscateTimes * (1f / obfuscateSpeed)))
            return;

        obfuscateTimes++;
        switch (obfuscateMode) {
            case LEFT -> text = text.replaceFirst("§k", "");
            case RIGHT -> {
                int index = text.lastIndexOf("§k");
                if (index != -1) {
                    text = text.substring(0, index) + text.substring(index + 2);
                }
            }
            case CENTER -> {
                int index = getClosestIndexToCenter(text, "§k");
                if (index != -1) {
                    text = text.substring(0, index) + text.substring(index + 2);
                }
            }
            case RANDOM -> {
                List<Integer> occurrences = new ArrayList<>();
                for (int i = 0; i <= text.length() - 2; i++) {
                    if (text.startsWith("§k", i)) {
                        occurrences.add(i);
                    }
                }

                if (!occurrences.isEmpty()) {
                    Random rand = new Random();
                    int index = occurrences.get(rand.nextInt(occurrences.size()));

                    text = text.substring(0, index) + text.substring(index + 2);
                }
            }
        }

    }

    private int getClosestIndexToCenter(String str, String target) {
        int centerIndex = str.length() / 2;
        int closestIndex = -1;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i <= str.length() - target.length(); i++) {
            if (str.substring(i, i + target.length()).equals(target)) {
                int distanceFromCenter = Math.abs(i - centerIndex);
                if (distanceFromCenter < minDistance) {
                    closestIndex = i;
                    minDistance = distanceFromCenter;
                }
            }
        }
        return closestIndex;
    }

}
