package antplutomigrator.runner;

import antplutomigrator.generate.*;
import antplutomigrator.generate.anthelpers.NoExpansionPropertyHelper;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.*;
import antplutomigrator.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by manuel on 31.01.17.
 */
public class AntMigrator {
    private static Log log = LogFactory.getLog(AntMigrator.class);

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
        options.addOption(Option.builder("c")
                .longOpt("continueOnError")
                .desc("Continue building on (certain) errors.")
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
            log.fatal("Unexpected exception.", exp);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ant2pluto", options);
            return;
        }

        List<String> libs = new ArrayList<>();
        libs.add("/usr/local/Cellar/ant/1.9.7/libexec/lib");
        libs.add("/Users/manuel/.m2/repository/junit/junit/4.12/");
        //LauncherHelpers.prepareClassLoader(null, libs);

        Project project = new Project();
        File buildFile = new File(line.getOptionValue("bf"));
        project.init();
        NoExpansionPropertyHelper.getPropertyHelper(project);
        ProjectHelper.configureProject(project, buildFile);

        Map<String, String> files = new HashMap<>();

        // Deal with all macros first, as evaluated macros are needed for the rest of the migration...
        for (Target target : project.getTargets().values()) {
            for (Task task : target.getTasks()) {
                generateMacro((UnknownElement) task, project, files, line);
            }
        }

        for (Target target : project.getTargets().values()) {
            if (!target.getName().isEmpty()) {
                NamingManager namingManager = new NamingManager();
                BuilderGenerator generator = new BuilderGenerator(line.getOptionValue("pkg"), project, target, !line.hasOption("noFD"), line.hasOption("c"));
                files.put(namingManager.getClassNameFor(StringUtils.capitalize(target.getName())) + "Builder.java", generator.getPrettyPrint());
            }
        }

        NamingManager namingManager = new NamingManager();

        BuilderInputGenerator builderInputGenerator = new BuilderInputGenerator(line.getOptionValue("pkg"), project.getName(), project, buildFile.getParentFile(), line.hasOption("c"));
        files.put(namingManager.getClassNameFor(project.getName()) + "Input.java", builderInputGenerator.getPrettyPrint());

        String plutoBuildListener = new String(Files.readAllBytes(Paths.get(AntMigrator.class.getResource("PlutoBuildListener.java").toURI())));
        files.put("PlutoBuildListener.java", plutoBuildListener.replace("<pkg>", line.getOptionValue("pkg")));
        String plutoPropertyHelper = new String(Files.readAllBytes(Paths.get(AntMigrator.class.getResource("PlutoPropertyHelper.java").toURI())));
        files.put("PlutoPropertyHelper.java", plutoPropertyHelper.replace("<pkg>", line.getOptionValue("pkg")));
        String consumer = new String(Files.readAllBytes(Paths.get(AntMigrator.class.getResource("BiConsumer.java").toURI())));
        files.put("BiConsumer.java", consumer.replace("<pkg>", line.getOptionValue("pkg")));

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

    private static void generateMacro(UnknownElement element, Project project, Map<String, String> files, CommandLine line) {
        if (element.getTaskName().equals("macrodef")) {
            // We have a macro. Use a MacroGenerator to antplutomigrator.generate a class for it
            NamingManager namingManager = new NamingManager();
            PropertyResolver resolver = new PropertyResolver(project, "input");
            // Exceute to add to the type table
            element.perform();
            MacroGenerator macroGenerator = new MacroGenerator(line.getOptionValue("pkg"), project, namingManager, resolver, element, line.hasOption("c"));
            files.put("macros/" + macroGenerator.getName() + ".java", macroGenerator.getPrettyPrint());
        } else {
            if (element.getChildren() != null) {
                for (UnknownElement c : element.getChildren()) {
                    generateMacro(c, project, files, line);
                }
            }
        }
    }

    private static void writeJavaFile(String baseDir, String pkg, String name, String content) throws IOException {
        Path dir = Paths.get(baseDir);
        dir = dir.resolve(pkg.replace(".", "/"));
        Files.createDirectories(dir);
        Path macrosDir = dir.resolve("macros");
        Files.createDirectories(macrosDir);

        Path file = dir.resolve(StringUtils.capitalize(name));
        log.info("Writing: " + file);
        Files.write(file, content.getBytes());
    }
}
