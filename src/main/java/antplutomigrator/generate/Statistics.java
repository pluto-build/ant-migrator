package antplutomigrator.generate;

import org.apache.tools.ant.UnknownElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistics {

  private static Statistics instance;
  private List<UnknownElement> generatedElements = new ArrayList<>();
  private List<UnknownElement> defaultGeneratedElements = new ArrayList<>();

  private Statistics() {}

  public static Statistics getInstance() {
    if (instance == null) instance = new Statistics();
    return instance;
  }

  public void generatedElement(UnknownElement element) {
    generatedElements.add(element);
  }
  public void defaultGenerated(UnknownElement element) { defaultGeneratedElements.add(element); }

  public void printStatistics() {
    HashMap<String, Integer> migratedTasks = new HashMap<>();
    for (UnknownElement element : generatedElements) {
      String taskName = element.getTaskName();
      migratedTasks.put(taskName, migratedTasks.getOrDefault(taskName, 0) + 1);
    }

    HashMap<String, Integer> defaultMigratedTasks = new HashMap<>();
    for (UnknownElement element : defaultGeneratedElements) {
      String taskName = element.getTaskName();
      defaultMigratedTasks.put(taskName, defaultMigratedTasks.getOrDefault(taskName, 0) + 1);
    }

    System.out.println("Migration statistics:\n------------------------");
    List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(migratedTasks.entrySet());
    sortedEntries.sort((t1, t2) -> t2.getValue().compareTo(t1.getValue()));
    for (Map.Entry<String, Integer> migratedTask : sortedEntries) {
      System.out.println(migratedTask.getKey() + ": " + migratedTask.getValue());
    }
    System.out.println("Default translation:\n------------------------");
    List<Map.Entry<String, Integer>> sortedDefaultEntries = new ArrayList<>(defaultMigratedTasks.entrySet());
    sortedDefaultEntries.sort((t1, t2) -> t2.getValue().compareTo(t1.getValue()));
    for (Map.Entry<String, Integer> migratedTask : sortedDefaultEntries) {
      System.out.println(migratedTask.getKey() + ": " + migratedTask.getValue());
    }
  }
}
