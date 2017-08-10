package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by manuel on 08.06.17.
 */
public class RunCommandTask extends TestTask {
    private final String command;
    private final File workingDir;

    public RunCommandTask(File workingDir, String command) {
        this.workingDir = workingDir;
        this.command = command;
    }

    @Override
    public String getDescription() {
        return "Running " + command;
    }

    @Override
    public void execute() throws Exception {
        List<String> commands = new ArrayList<>();

        boolean inCommand = false;
        String current = "";
        for (int i = 0; i < command.length(); i++){
            char c = command.charAt(i);
            //Process char
            if (c == ' ' && !inCommand) {
                if (!current.equals(""))
                    commands.add(current);
                current = "";
            } else if (c == '\"') {
                inCommand = !inCommand;
                if (!current.equals(""))
                    commands.add(current);
                current = "";
            } else {
                current += c;
            }
        }
        if (!current.equals(""))
            commands.add(current);

        Process antProcess = new ProcessBuilder().directory(workingDir).command(commands).inheritIO().start();
        assertEquals(0, antProcess.waitFor());
    }
}
