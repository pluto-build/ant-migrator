package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

public class MakePropertiesRelativeTask extends TestTask {

    private File buildFile;

    public MakePropertiesRelativeTask(File buildFile) {
        this.buildFile = buildFile;
    }

    @Override
    public String getDescription() {
        return "Removing absolute path properties in " + buildFile.getAbsolutePath();
    }

    @Override
    public void execute() throws Exception {
        List<String> lines = FileUtils.readLines(buildFile);
        boolean changed = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("property") && line.contains("location=\"")) {
                lines.set(i, line.replace("location=\"", "value=\""));
                changed = true;
            }
        }
        if (changed)
            FileUtils.writeLines(buildFile, lines);
    }
}
