package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class CollectBedTask extends CraftWithMatchingWoolTask {

    public static final Block[] BEDS = ItemHelper.itemsToBlocks(ItemHelper.BED);

    private final ItemTarget _visualBedTarget;

    public CollectBedTask(Item[] beds, ItemTarget wool, int count) {
        // Top 3 are wool, must be the same.
        super(
            beds.length > 1 ? new ItemTarget(beds, count, "any bed") : new ItemTarget(beds, count),
            colorfulItems -> colorfulItems.wool,
            colorfulItems -> colorfulItems.bed,
            createBedRecipe(wool),
            new boolean[]{true, true, true, false, false, false, false, false, false}
        );
        _visualBedTarget = beds.length > 1 ? new ItemTarget(beds, count, "any bed") : new ItemTarget(beds, count);
    }

    public CollectBedTask(Item bed, String woolCatalogueName, int count) {
        this(new Item[]{bed}, new ItemTarget(woolCatalogueName, 1), count);
    }

    public CollectBedTask(int count) {
        this(ItemHelper.BED, TaskCatalogue.getItemTarget("wool", 1), count);
    }

    private static CraftingRecipe createBedRecipe(ItemTarget wool) {
        ItemTarget w = wool;
        ItemTarget p = TaskCatalogue.getItemTarget("planks", 1);
        return CraftingRecipe.newShapedRecipe(new ItemTarget[]{w, w, w, p, p, p, null, null, null}, 1);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(BEDS);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(BEDS);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Break beds from the world if possible, that would be pretty fast.
        if (mod.getBlockTracker().anyFound(BEDS)) {
            // Failure + blacklisting is encapsulated within THIS task
            return new MineAndCollectTask(new ItemTarget(ItemHelper.BED, 1, "any bed"), BEDS, MiningRequirement.HAND);
        }
        return new TimeoutWanderTask();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CollectBedTask task) {
            return task._visualBedTarget.equals(_visualBedTarget);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting a bed";
    }
}
