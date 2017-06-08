package antplutomigrator.correctness.utils;

/**
 * Created by manuel on 08.06.17.
 */
public abstract class TestTask {
    public abstract String getDescription();

    public abstract void execute() throws Exception;
}
