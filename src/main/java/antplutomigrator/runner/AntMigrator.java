package antplutomigrator.runner;

import antplutomigrator.generate.*;
import antplutomigrator.generate.anthelpers.NoExpansionPropertyHelper;
import javafx.beans.property.Property;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.*;
import antplutomigrator.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
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
        options.addOption(Option.builder("d")
                .longOpt("debug")
                .desc("Enable debug logging on generated code.")
                .build());
        options.addOption(Option.builder("t")
                .longOpt("target")
                .hasArg()
                .desc("Set executed the target.")
                .build());
        options.addOption(Option.builder("noIncrJavac")
                .desc("Use a non incremental Javac task version.")
                .build());
        options.addOption(Option.builder("calcStats")
                .desc("Calculate build statistics after run.")
                .build());
        options.addOption(Option.builder("migStats")
                .desc("Calculate migration statistics.")
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

        Project project = new Project();
        File buildFile = new File(line.getOptionValue("bf"));
        project.init();
        /* Doing this here breaks certain build files, as unexpanded properties might already be used to initialize other / remaining properties
            e.g. <property file="${basedir}/my.properties"/>
            Not doing this would already resolve all properties which is also undesirable...
        */
        NoExpansionPropertyHelper propertyHelper = NoExpansionPropertyHelper.getPropertyHelper(project);
        ProjectHelper.configureProject(project, buildFile);

        if (project.getName() == null) {
            project.setName("project");
        }

        Settings settings = Settings.getInstance();
        settings.setUseNoIncrJavac(line.hasOption("noIncrJavac"));
        settings.setCalculateStatistics(line.hasOption("calcStats"));
        settings.setCalculateMigrationStatistics(line.hasOption("migStats"));

        //project.initProperties();
        // Introduce this workaround to "reexceute" the necessary properties...
        // TODO: Maybe reexecute and reset every UnknownElement? To be more generally correct?
        Target defaultTarget = project.getTargets().get("");
        for (Task task: defaultTarget.getTasks()) {
            if (task instanceof UnknownElement) {
                UnknownElement element = (UnknownElement) task;
                String[] types = new String[] {"file", "url", "resource"};
                for (String type: types) {
                    if (element.getTaskName().equals("property") && element.getWrapper().getAttributeMap().containsKey(type)) {
                        String propertyFile = element.getWrapper().getAttributeMap().get(type).toString();
                        log.debug("found property "+type+": " + propertyFile);
                        element.getWrapper().removeAttribute(type);
                        String expanded = propertyHelper.reallyParseProperties(propertyFile).toString();
                        String oldExpanded = "";
                        while (!expanded.equals(oldExpanded)) {
                            oldExpanded = expanded;
                            expanded = propertyHelper.reallyParseProperties(expanded).toString();
                        }
                        element.getWrapper().setAttribute(type, expanded);
                        element.maybeConfigure();
                        element.execute();
                    }
                }
            }
        }


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
        files.put(namingManager.getClassNameFor(project.getName()) + "Context.java", builderInputGenerator.getPrettyPrint());

        String plutoBuildListener = readResource("/PlutoBuildListener.java");
        files.put("PlutoBuildListener.java", plutoBuildListener.replace("<pkg>", line.getOptionValue("pkg")));
        String plutoPropertyHelper = readResource("/PlutoPropertyHelper.java");
        files.put("PlutoPropertyHelper.java", plutoPropertyHelper.replace("<pkg>", line.getOptionValue("pkg")));
        String consumer = readResource("/BiConsumer.java");
        files.put("BiConsumer.java", consumer.replace("<pkg>", line.getOptionValue("pkg")));
        if (Settings.getInstance().isUseNoIncrJavac()) {
            String noIncrJavac = readResource("/NoIncrJavac.java");
            files.put("NoIncrJavac.java", noIncrJavac.replace("<pkg>", line.getOptionValue("pkg")));
        }
        if (Settings.getInstance().isCalculateStatistics()) {
            String statistics = readResource("/Statistics.java");
            files.put("Statistics.java", statistics.replace("<pkg>", line.getOptionValue("pkg")));
        }

        String targetName = project.getDefaultTarget();
        List<String> targets = new ArrayList<>();

        if (line.hasOption("t"))
            targets.addAll(Arrays.asList(line.getOptionValue("t").split(",")));
        else
            targets.add(targetName);

        BuilderMainGenerator mainGenerator = new BuilderMainGenerator(line.getOptionValue("pkg"), project.getName(), targets, !line.hasOption("noFD"), line.hasOption("d"));
        if (line.hasOption("m")) {
            files.put(namingManager.getClassNameFor(project.getName()) + ".java", mainGenerator.getPrettyPrint());
        }

        String antBuilder = readResource("/AntBuilder.java");
        String fd = "@Override\n" +
                "protected boolean useFileDependencyDiscovery() {\n" +
                "  return " + (!line.hasOption("noFD")) + ";\n" +
                "}";
        files.put("AntBuilder.java", antBuilder.replace("<pkg>", line.getOptionValue("pkg")).replace("<ctx>", mainGenerator.getName() + "Context").replace("<fd>", fd));


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

        if (Settings.getInstance().isCalculateMigrationStatistics()) {
            Statistics.getInstance().printStatistics();
        }
    }

    private static String readResource(String name) throws Exception {
        final URI uri = AntMigrator.class.getResource(name).toURI();
        if (uri.getScheme().equals("file"))
        {
            Path path = Paths.get(uri);
            return new String(Files.readAllBytes(path));
        }
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path path = Paths.get(uri);
            return new String(Files.readAllBytes(path));
        }
    }

    private static void generateMacro(UnknownElement element, Project project, Map<String, String> files, CommandLine line) {
        if (element.getTaskName().equals("macrodef")) {
            // We have a macro. Use a MacroGenerator to antplutomigrator.generate a class for it
            NamingManager namingManager = new NamingManager();
            PropertyResolver resolver = new PropertyResolver(project, "context");
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

        Path file = dir.resolve(name);
        log.info("Writing: " + file);
        Files.write(file, content.getBytes());
    }
}
