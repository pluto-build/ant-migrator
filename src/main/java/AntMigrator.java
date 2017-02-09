import generate.BuilderGenerator;
import generate.BuilderInputGenerator;
import generate.BuilderMainGenerator;
import generate.NamingManager;
import generate.anthelpers.NoExpansionPropertyHelper;
import org.apache.commons.cli.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
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
        options.addOption(Option.builder("od")
                .longOpt("outputDirectory")
                .required(false)
                .hasArg()
                .desc("Specifies output directory for the produced pluto buildfiles (Prints to console if not specified).")
                .build());
        options.addOption(Option.builder("m")
                .longOpt("main")
                .desc("Generate a main class")
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
        NoExpansionPropertyHelper.getPropertyHelper(project);
        ProjectHelper.configureProject(project, buildFile);


        Map<String, String> files = new HashMap<>();
        for (Target target : project.getTargets().values()) {
            if (!target.getName().isEmpty()) {
                NamingManager namingManager = new NamingManager();
                BuilderGenerator generator = new BuilderGenerator(line.getOptionValue("pkg"), target.getName() + "Builder", project.getName(), !line.hasOption("noFD"));
                generator.setCommands(Arrays.asList(target.getTasks()));
                generator.setDependentBuilders(Collections.list(target.getDependencies()));
                files.put(namingManager.getClassNameFor(StringUtils.capitalize(target.getName())) + "Builder.java", generator.getPrettyPrint());
            }
        }

        NamingManager namingManager = new NamingManager();

        BuilderInputGenerator builderInputGenerator = new BuilderInputGenerator(line.getOptionValue("pkg"), project.getName() + "Input", project);
        files.put(namingManager.getClassNameFor(project.getName()) + "Input.java", builderInputGenerator.getPrettyPrint());

        String plutoBuildListener = new String(Files.readAllBytes(Paths.get(AntMigrator.class.getResource("PlutoBuildListener.java").toURI())));
        files.put("PlutoBuildListener.java", plutoBuildListener.replace("<pkg>", line.getOptionValue("pkg")));

        if (line.hasOption("m")) {
            BuilderMainGenerator mainGenerator = new BuilderMainGenerator(line.getOptionValue("pkg"), project.getName(), project.getDefaultTarget());
            files.put(namingManager.getClassNameFor(project.getName()) + ".java", mainGenerator.getPrettyPrint());
        }

        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (line.hasOption("od")) {
                writeJavaFile(line.getOptionValue("od"), line.getOptionValue("pkg"), entry.getKey(), entry.getValue());
            } else {
                System.out.println(entry.getKey());
                System.out.println("------------------------");
                System.out.println(entry.getValue());
                System.out.println();
            }
        }
    }

    private static void writeJavaFile(String baseDir, String pkg, String name, String content) throws IOException {
        Path dir = Paths.get(baseDir);
        dir = dir.resolve(pkg.replace(".", "/"));
        Files.createDirectories(dir);

        Path file = dir.resolve(StringUtils.capitalize(name));
        System.out.println("Writing: " + file);
        Files.write(file, content.getBytes());
    }
}
