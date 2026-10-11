package org.wilkretawesomesauce.alchemancystuck.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonAttachments;

/**
 * The clone's melee swing (see {@code MeleeAttackGoal#checkAndPerformAttack}, which calls
 * {@code mob.swing(...)} like any vanilla mob) and {@code HumanoidModel}'s attack-arm animation both
 * already work out of the box - neither needed anything custom. What was missing is that
 * {@code RageEffect#startRage} only ever calls {@code owner.setCamera(clone)}; it never touches the
 * client's camera *type*, so the player stays in whatever perspective they were already in - first
 * person by default, which (like a real player's own first-person view) doesn't render the possessed
 * entity's own body/arms at all. Forcing third-person while possessing is what actually makes the
 * swing visible, not a fix to the swing itself.
 */
@EventBusSubscriber(modid = Alchemancystuck.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RageCameraHandler {

    private static boolean wasPossessing = false;

    /**
     * The one place this condition is spelled out - {@code RageHudMixin} reuses this rather than
     * duplicating it, since "possessing" here specifically means camera-locked onto a live rage clone,
     * not just "gamemode happens to be spectator" (a player who opened spectator mode themselves, with
     * no clone involved, shouldn't have their HUD touched by either of these).
     *
     * <p>Checks {@link AddonAttachments#IS_RAGE_CLONE} rather than {@code instanceof RageCloneEntity} -
     * that class only exists server-side; the client mirrors any player-type entity that isn't its own
     * local player as a generic {@code RemotePlayer}; regardless of what the entity's real server-side
     * Java type is, an {@code instanceof} check against it client-side is always false. The attachment
     * is a synced marker {@code RageEffect#startRage} sets on the clone precisely so the client can
     * still tell the two apart.
     */
    public static boolean isPossessing(Minecraft client) {
        Entity camera = client.getCameraEntity();
        return client.player != null && client.player.isSpectator() && camera != null && camera.getData(AddonAttachments.IS_RAGE_CLONE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        boolean possessing = isPossessing(client);

        if (possessing) {
            if (client.options.getCameraType().isFirstPerson())
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (wasPossessing) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }

        wasPossessing = possessing;
    }
}
