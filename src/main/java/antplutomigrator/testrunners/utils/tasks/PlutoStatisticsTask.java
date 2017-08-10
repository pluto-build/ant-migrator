package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import build.pluto.BuildUnit;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlutoStatisticsTask extends TestTask {

    private final List<File> depFiles;

    public PlutoStatisticsTask(List<File> depFiles) {
        this.depFiles = depFiles;
    }

    @Override
    public String getDescription() {
        return "Getting statistics for " + depFiles;
    }

    @Override
    public void execute() throws Exception {
        Set<BuildUnit<?>> allUnits = new HashSet<>();
        for (File depFile: depFiles) {
            BuildUnit<?> unit = BuildUnit.read(depFile);
            allUnits.addAll(unit.getTransitiveModuleDependencies());
        }
        int totalRequiredFiles = 0;
        int totalRequiredBuilders = 0;
        int totalProvidedFiles = 0;
        for (BuildUnit<?> unit: allUnits) {
            totalProvidedFiles += unit.getGeneratedFiles().size();
            totalRequiredFiles += unit.getRequiredFiles().size();
            totalRequiredBuilders += unit.getModuleDependencies().size();
        }
        System.out.println("Total build units:        " + allUnits.size());
        System.out.println("Total required files:     " + totalRequiredFiles);
        System.out.println("Total required builders:  " + totalRequiredBuilders);
        System.out.println("Total provided files:     " + totalProvidedFiles);
    }
}
