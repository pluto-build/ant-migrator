package antplutomigrator.runner;

import org.apache.tools.ant.launch.Launcher;

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
