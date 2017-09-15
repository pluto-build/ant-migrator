package antplutomigrator.generate;

public class Settings {
  private static Settings instance = new Settings();
  private boolean useNoIncrJavac;
  private boolean calculateStatistics;
  private boolean calculateMigrationStatistics;

  private Settings() {}

  public static Settings getInstance() {
    return instance;
  }

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

  public boolean isCalculateMigrationStatistics() {
    return calculateMigrationStatistics;
  }

  public void setCalculateMigrationStatistics(boolean calculateMigrationStatistics) {
    this.calculateMigrationStatistics = calculateMigrationStatistics;
  }
}
