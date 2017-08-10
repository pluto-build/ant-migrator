package antplutomigrator.testrunners.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 08.06.17.
 */
public class TaskExecutor {
    private List<TestTask> tasks = new ArrayList();

    public void addTask(TestTask task) {
        tasks.add(task);
    }

    public void executeTasks() throws Exception {
        for (TestTask t: tasks) {
            System.out.println("-----------------------------");
            System.out.println("Executing: " + t.getDescription());
            long ctm = System.currentTimeMillis();
            t.execute();
            long time = System.currentTimeMillis()-ctm;
            System.out.println("  => took " + time + "ms.");
            System.out.println();
        }
    }
}
