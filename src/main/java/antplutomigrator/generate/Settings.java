package antplutomigrator.generate;

public class Settings {
    private boolean useNoIncrJavac;
    private boolean calculateStatistics;

    public boolean isUseNoIncrJavac() {
        return useNoIncrJavac;
    }

    public void setUseNoIncrJavac(boolean useNoIncrJavac) {
        this.useNoIncrJavac = useNoIncrJavac;
    }

    public boolean isCalculateStatistics() {
        return calculateStatistics;
    }

    public void setCalculateStatistics(boolean calculateStatistics) {
        this.calculateStatistics = calculateStatistics;
    }

    private Settings() { }

    private static Settings instance = new Settings();

    public static Settings getInstance() {
        return instance;
    }
}
