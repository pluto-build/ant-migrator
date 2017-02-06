import generate.BuilderGenerator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by manuel on 31.01.17.
 */
public class AntMigrator {
    public static void main(String[] args) throws Exception {
        Project project = new Project();
        File buildFile = new File(args[0]);
        project.init();
        ProjectHelper.configureProject(project, buildFile);


        Map<String, String> targets = new HashMap<>();
        for (Target target : project.getTargets().values()) {
            if (!target.getName().isEmpty()) {
                BuilderGenerator generator = new BuilderGenerator("build.pluto.plutoanttester", target.getName() + "Builder", false);
                generator.setCommands(Arrays.asList(target.getTasks()));
                generator.setDependentBuilders(Collections.list(target.getDependencies()));
                targets.put(target.getName(), generator.getPrettyPrint());
            }
        }

        System.out.println(targets);
    }
}
