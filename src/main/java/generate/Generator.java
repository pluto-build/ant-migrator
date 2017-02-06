package generate;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by manuel on 29.11.16.
 */
public abstract class Generator {
    private int indent = 0;
    private StringBuilder builder = new StringBuilder();
    private List<String> closingStrings = new ArrayList<>();

    List<Pair<Integer, Supplier<String>>> laterList = new ArrayList<>();

    public String getPrettyPrint() {
        generatePrettyPrint();
        laterList.sort((e1, e2) -> Integer.compare(e2.getKey(), e1.getKey()));
        laterList.forEach((e) ->
                builder.insert(e.getKey(), e.getValue().get())
        );
        return builder.toString();
    }

    public void generatePrettyPrint() {
        indent = 0;
        builder = new StringBuilder();
        closingStrings = new ArrayList<>();
    }

    @Override
    public String toString() {
        return getPrettyPrint();
    }

    protected void increaseIndentation(int level) {
        indent += level;
    }

    private String getIndentString() {
        String indentString = "";
        for (int i = 0; i < indent; i++)
            indentString += "  ";
        return indentString;
    }

    protected void printLater(Supplier<String> s) {
        laterList.add(new Pair(builder.length(), s));
    }

    protected void printString(String str) {
        Arrays.asList(str.split("\n")).forEach(line ->
                builder.append(getIndentString()).append(line).append("\n")
        );
    }

    protected void printString(String str, String closing) {
        printString(str);
        closingStrings.add(0, closing);
    }

    protected void closeOneLevel() {
        increaseIndentation(-1);
        builder.append(getIndentString()).append(closingStrings.get(0)).append("\n");
        closingStrings.remove(0);
    }
}
