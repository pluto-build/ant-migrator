package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplaceInLinesTask extends TestTask {

    private File file;
    private HashMap<String, String> replacements;

    public ReplaceInLinesTask(File file, HashMap<String, String> replacements) {
        this.file = file;
        this.replacements = replacements;
    }

    @Override
    public String getDescription() {
        return "Replacing in file " + file;
    }

    @Override
    public void execute() throws Exception {
        List<String> lines = FileUtils.readLines(file);
        boolean changed = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (Map.Entry<String, String> entry: replacements.entrySet()) {
                if (line.contains(entry.getKey())) {
                    lines.set(i, line.replace(entry.getKey(), entry.getValue()));
                    changed = true;
                }
            }
        }
        if (changed)
            FileUtils.writeLines(file, lines);
    }
}
