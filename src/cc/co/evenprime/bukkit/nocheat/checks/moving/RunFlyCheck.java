package cc.co.evenprime.bukkit.nocheat.checks.moving;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import cc.co.evenprime.bukkit.nocheat.NoCheat;
import cc.co.evenprime.bukkit.nocheat.config.Permissions;
import cc.co.evenprime.bukkit.nocheat.config.cache.ConfigurationCache;
import cc.co.evenprime.bukkit.nocheat.data.MovingData;

/**
 * The main Check class for Move event checking. It will decide which checks
 * need to be executed and in which order. It will also precalculate some values
 * that are needed by multiple checks.
 * 
 * @author Evenprime
 * 
 */
public class RunFlyCheck {

    private final FlyingCheck       flyingCheck;
    private final RunningCheck      runningCheck;
    private final NoFallCheck       noFallCheck;
    private final MorePacketsCheck  morePacketsCheck;

    private final MovingEventHelper helper;

    public RunFlyCheck(NoCheat plugin) {
        this.helper = new MovingEventHelper();

        this.flyingCheck = new FlyingCheck(plugin);
        this.noFallCheck = new NoFallCheck(plugin);
        this.runningCheck = new RunningCheck(plugin, noFallCheck);
        this.morePacketsCheck = new MorePacketsCheck(plugin);
    }

    /**
     * Return the a new destination location or null
     * 
     * @param event
     * @return
     */
    public Location check(final Player player, final Location from, final Location to, final MovingData data, final ConfigurationCache cc) {

        // Players in vehicles are of no interest
        if(player.isInsideVehicle())
            return null;

        /**
         * If not null, this will be used as the new target location
         */
        Location newToLocation = null;

        /******** DO GENERAL DATA MODIFICATIONS ONCE FOR EACH EVENT *****/
        if(data.horizVelocityCounter > 0) {
            data.horizVelocityCounter--;
        } else {
            data.horizFreedom *= 0.90;
        }

        if(data.vertVelocityCounter > 0) {
            data.vertVelocityCounter--;
            data.vertFreedom += data.vertVelocity;
            data.vertVelocity *= 0.90;
        } else {
            data.vertFreedom = 0;
        }

        /************* DECIDE WHICH CHECKS NEED TO BE RUN *************/
        final boolean runflyCheck = cc.moving.runflyCheck && !player.hasPermission(Permissions.MOVE_RUNFLY);
        final boolean flyAllowed = cc.moving.allowFlying || player.hasPermission(Permissions.MOVE_FLY) || (player.getGameMode() == GameMode.CREATIVE && cc.moving.identifyCreativeMode);
        final boolean morepacketsCheck = cc.moving.morePacketsCheck && !player.hasPermission(Permissions.MOVE_MOREPACKETS);

        /********************* EXECUTE THE FLY/JUMP/RUNNING CHECK ********************/
        // If the player is not allowed to fly and not allowed to run
        if(runflyCheck) {
            if(flyAllowed) {
                newToLocation = flyingCheck.check(player, from, to, cc, data);
            }
            else {
                newToLocation = runningCheck.check(player, from, to, helper, cc, data);
            }
        }

        /********* EXECUTE THE MOREPACKETS CHECK ********************/

        if(newToLocation == null && morepacketsCheck) {
            newToLocation = morePacketsCheck.check(player, cc, data);
        }
        
        return newToLocation;
    }

    /**
     * This is a workaround for people placing blocks below them causing false positives
     * with the move check(s).
     * 
     * TODO: Check if still needed, maybe this got fixed in 1.8.1
     * 
     * @param player
     * @param data
     * @param blockPlaced
     */
    public void blockPlaced(Player player, MovingData data, Block blockPlaced) {

        if(blockPlaced == null || data.runflySetBackPoint == null) {
            return;
        }

        Location lblock = blockPlaced.getLocation();
        Location lplayer = player.getLocation();

        if(Math.abs(lplayer.getBlockX() - lblock.getBlockX()) <= 1 && Math.abs(lplayer.getBlockZ() - lblock.getBlockZ()) <= 1 && lplayer.getBlockY() - lblock.getBlockY() >= 0 && lplayer.getBlockY() - lblock.getBlockY() <= 2) {

            int type = helper.types[blockPlaced.getTypeId()];
            if(helper.isSolid(type) || helper.isLiquid(type)) {
                if(lblock.getBlockY() + 1 >= data.runflySetBackPoint.getY()) {
                    data.runflySetBackPoint.setY(lblock.getBlockY() + 1);
                    data.jumpPhase = 0;
                }
            }
        }
    }
}