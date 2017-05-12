import generate.anthelpers.NoExpansionPropertyHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.launch.Launcher;

import java.io.File;

/**
 * Created by manuel on 16.02.17.
 */
public class AntTestingMain {

    public static void main(String[] args) {
        /*Project project = new Project();
        File buildFile = new File(args[1]);
        project.init();
        //NoExpansionPropertyHelper.getPropertyHelper(project);
        ProjectHelper.configureProject(project, buildFile);
        project.getTargets().get("par").performTasks();

        System.out.println(project);
        project.executeTarget("par");*/
        Launcher.main(args);
    }
}
