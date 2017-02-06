import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Echo;

import java.io.File;
import java.util.*;

public class ParserMain {
    public static void main(String[] args) throws Exception {
        Project project = new Project();
        project.addBuildListener(new BuildListener() {
            @Override
            public void buildStarted(BuildEvent event) {

            }

            @Override
            public void buildFinished(BuildEvent event) {

            }

            @Override
            public void targetStarted(BuildEvent event) {

            }

            @Override
            public void targetFinished(BuildEvent event) {

            }

            @Override
            public void taskStarted(BuildEvent event) {
                System.out.println("Starting: " + event.getTask().getTaskName());
            }

            @Override
            public void taskFinished(BuildEvent event) {

            }

            @Override
            public void messageLogged(BuildEvent event) {
                System.out.println(event.getMessage());
            }
        });
        File buildFile = new File(args[0]);
        project.init();
        ProjectHelper.configureProject(project, buildFile);

        System.out.println(project.getTargets());
        System.out.println("All target count: " + project.getTargets().size());

        Set<Task> tasks = new HashSet<>();
        project.getTargets().forEach((tn, target) -> tasks.addAll(Arrays.asList(target.getTasks())));
        System.out.println("Task count: "+ tasks.size());

        Target def = project.getTargets().get(project.getDefaultTarget());

        ParserMain parserMain = new ParserMain(project);

        Set<String> reachableTargets = parserMain.reachableTargets(def);

        System.out.println(reachableTargets);
        System.out.println("Reachable Targets from default target count: " + reachableTargets.size());

        tasks.clear();
        reachableTargets.forEach(target -> tasks.addAll(Arrays.asList(project.getTargets().get(target).getTasks())));

        System.out.println("Reachable Task count: "+ tasks.size());

        //Try to run first task
        UnknownElement first = (UnknownElement)def.getTasks()[1];
        first.maybeConfigure();
        first.execute();

        Echo echo = new Echo();
        echo.setMessage("Hello, World 2!");
        echo.execute();

        System.out.println(first.getWrapper().getAttributeMap());
    }

    Project project;

    public ParserMain(Project project) {
        this.project = project;
    }

    public Set<String> reachableTargets(Target target) {
        Set<String> result = new HashSet<>();

        for (Enumeration<String> enumeration = target.getDependencies(); enumeration.hasMoreElements();) {
            String dep = enumeration.nextElement();
            result.add(dep);
            result.addAll(reachableTargets(project.getTargets().get(dep)));
        }
        return result;
    }


    private static int printAllDependencies(Project project,  Target target, int level) {
        int count = 1;
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
        System.out.println(target.getName());
        for (Task task: target.getTasks()) {
            UnknownElement element = (UnknownElement)task;
            element.maybeConfigure();
            for (int i = 0; i < level; i++) {
                System.out.print(" ");
            }
            System.out.print(" -> ");
            System.out.print(element.getTaskName());
            System.out.print(": ");
            System.out.print(element.getTaskType());
            System.out.print(" (");
            System.out.print(element.getDescription());
            System.out.println(")");
        }
        for (Enumeration<String> enumeration = target.getDependencies(); enumeration.hasMoreElements();) {
            count += printAllDependencies(project, project.getTargets().get(enumeration.nextElement()), level+1);
        }
        return count;
    }
}
