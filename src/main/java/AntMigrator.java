import generate.BuilderGenerator;
import org.apache.commons.cli.*;
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
        Options options = new Options();
        options.addOption(Option.builder("h")
                .longOpt("help")
                .required(false)
                .desc("Shows this message")
                .build());
        options.addOption(Option.builder("bf")
                .longOpt("buildFile")
                .required()
                .hasArg()
                .desc("The Buildfile to parse")
                .build());
        options.addOption(Option.builder("pkg")
                .longOpt("package")
                .required()
                .desc("The java package name of the final pluto build script")
                .hasArg()
                .build());
        options.addOption(Option.builder("noFD")
                .longOpt("noFileDependencyDiscovery")
                .desc("Switches off file dependency discovery (not recommended).")
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine line;
        try {
            // parse the command line arguments
            line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ant2pluto", options);
            }
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ant2pluto", options);
            return;
        }


        Project project = new Project();
        File buildFile = new File(line.getOptionValue("bf"));
        project.init();
        ProjectHelper.configureProject(project, buildFile);


        Map<String, String> targets = new HashMap<>();
        for (Target target : project.getTargets().values()) {
            if (!target.getName().isEmpty()) {
                BuilderGenerator generator = new BuilderGenerator(line.getOptionValue("pkg"), target.getName() + "Builder", !line.hasOption("noFD"));
                generator.setCommands(Arrays.asList(target.getTasks()));
                generator.setDependentBuilders(Collections.list(target.getDependencies()));
                targets.put(target.getName(), generator.getPrettyPrint());
            }
        }

        System.out.println(targets);
    }
}
