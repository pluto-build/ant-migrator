package <pkg>;

import build.pluto.dependency.FileRequirement;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.BuildUnit;

import java.io.IOException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

class Statistics {
    public static void calculateStatistics(BuildRequest<?, ?, ?, ?> req) {
        try {
            BuildUnit<?> mainUnit = BuildManagers.readResult(req);

            Set<BuildUnit<?>> allUnits = mainUnit.getTransitiveModuleDependencies();
            Set<File> uniqueRequired = new HashSet<>();
            Set<File> uniqueProvided = new HashSet<>();

            int totalRequiredFiles = 0;
            int totalRequiredBuilders = 0;
            int totalProvidedFiles = 0;
            for (BuildUnit<?> unit : allUnits) {
                System.out.println(unit + " required: " + unit.getRequiredFiles().size() + " provided: " + unit.getGeneratedFiles().size());
                uniqueProvided.addAll(unit.getGeneratedFiles());
                totalProvidedFiles += unit.getGeneratedFiles().size();
                for (FileRequirement fr: unit.getRequiredFiles()) {
                    uniqueRequired.add(fr.file);
                }
                totalRequiredFiles += unit.getRequiredFiles().size();
                totalRequiredBuilders += unit.getModuleDependencies().size();
            }
            System.out.println("Total build units:        " + allUnits.size());
            System.out.println("Total required files:     " + totalRequiredFiles + " (unique: " + uniqueRequired.size() + ")");
            System.out.println("Total required builders:  " + totalRequiredBuilders);
            System.out.println("Total provided files:     " + totalProvidedFiles + " (unique: " + uniqueProvided.size() + ")");
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}