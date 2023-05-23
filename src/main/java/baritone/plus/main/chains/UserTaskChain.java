package baritone.plus.main.chains;

import baritone.plus.main.BaritonePlus;
import baritone.plus.main.Debug;
import baritone.plus.brain.BaritoneBrain;
import baritone.plus.api.event.EventBus;
import baritone.plus.api.event.events.TaskFinishedEvent;
import baritone.plus.api.tasks.Task;
import baritone.plus.api.tasks.TaskRunner;
import baritone.plus.api.util.time.Stopwatch;

import java.time.LocalDateTime;

// A task chain that runs a user defined task at the same priority.
// This basically replaces our old Task Runner.
@SuppressWarnings("ALL")
public class UserTaskChain extends SingleTaskChain {

    private final Stopwatch _taskStopwatch = new Stopwatch();
    private Runnable _currentOnFinish = null;

    private boolean _runningIdleTask;
    private boolean _nextTaskIdleFlag;

    public UserTaskChain(TaskRunner runner) {
        super(runner);
    }

    private static String prettyPrintTimeDuration(double seconds) {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        int days = hours / 24;

        String result = "";
        if (days != 0) {
            result += days + " days ";
        }
        if (hours != 0) {
            result += (hours % 24) + " hours ";
        }
        if (minutes != 0) {
            result += (minutes % 60) + " minutes ";
        }
        if (!result.equals("")) {
            result += "and ";
        }
        result += String.format("%.3f", (seconds % 60));
        return result;
    }

    @Override
    protected void onTick(BaritonePlus mod) {

        // Pause if we're not loaded into a world.
        if (!mod.inGame()) return;

        super.onTick(mod);
    }

    public void cancel(BaritonePlus mod) {
        if (_mainTask != null && _mainTask.isActive()) {
            stop(mod);
            onTaskFinish(mod);
        }
    }

    @Override
    public float getPriority(BaritonePlus mod) {
        return 50;
    }

    @Override
    public String getName() {
        return "User Tasks";
    }

    public void runTask(BaritonePlus mod, Task task, Runnable onFinish) {
        _runningIdleTask = _nextTaskIdleFlag;
        _nextTaskIdleFlag = false;

        _currentOnFinish = onFinish;

        if (!_runningIdleTask) {
            Debug.logMessage("User Task Set: " + task.toString());
        }
        mod.getTaskRunner().enable();
        _taskStopwatch.begin();
        setTask(task);

        if (mod.getModSettings().failedToLoad()) {
            Debug.logWarning("Settings file failed to load at some point. Check logs for more info, or delete the" +
                    " file to re-load working settings.");
        }
    }

    @Override
    protected void onTaskFinish(BaritonePlus mod) {
        boolean shouldIdle = mod.getModSettings().shouldRunIdleCommandWhenNotActive();
        if (!shouldIdle) {
            // Stop.
            mod.getTaskRunner().disable();
            // Extra reset. Sometimes baritone is laggy and doesn't properly reset our press
            mod.getClientBaritone().getInputOverrideHandler().clearAllKeys();
        }
        double seconds = _taskStopwatch.time();
        Task oldTask = _mainTask;
        _mainTask = null;
        if (_currentOnFinish != null) {
            //noinspection unchecked
            _currentOnFinish.run();
        }
        // our `onFinish` might have triggered more tasks.
        boolean actuallyDone = _mainTask == null;
        if (actuallyDone) {
            if (!_runningIdleTask) {
                var text = String.format("User Task: {%s} FINISHED. Took %s seconds.", oldTask == null ? "null" : oldTask.getDebugState(), prettyPrintTimeDuration(seconds));
                Debug.logMessage(text);
                // Save to Memory
                mod.getBaritoneBrain().memory.add(
                        new BaritoneBrain.MemoryItem(
                                text,
                                LocalDateTime.now(),
                                (int) (seconds % 2))
                );
                EventBus.publish(new TaskFinishedEvent(seconds, oldTask));
            }
            if (shouldIdle) {
//                AltoClef.getCommandExecutor().executeWithPrefix(mod.getModSettings().getIdleCommand());
                signalNextTaskToBeIdleTask();
                _runningIdleTask = true;
            }
        }
    }

    public boolean isRunningIdleTask() {
        return isActive() && _runningIdleTask;
    }

    // The next task will be an idle task.
    public void signalNextTaskToBeIdleTask() {
        _nextTaskIdleFlag = true;
    }
}
