package iot.zjt.floptimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FLOptimizer {
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 26; i++) {
            optimize("Chart_" + i, "org.jfree.");
        }

        for (int i = 1; i <= 40; i++) {
            if (i == 6) {
                continue;
            }
            optimize("Cli_" + i, "org.apache.commons.cli.");
        }

        for (int i = 1; i <= 176; i++) {
            if (i == 63 || i == 93) {
                continue;
            }
            optimize("Closure_" + i, "com.google.javascript.");
        }

        for (int i = 1; i <= 18; i++) {
            optimize("Codec_" + i, "org.apache.commons.codec.");
        }

        for (int i = 25; i <= 28; i++) {
            try {
                optimize("Collections_" + i, "org.apache.commons.collections4");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 1; i <= 47; i++) {
            optimize("Compress_" + i, "org.apache.commons.compress.");
        }

        for (int i = 1; i <= 16; i++) {
            optimize("Csv_" + i, "org.apache.commons.csv.");
        }

        for (int i = 1; i <= 18; i++) {
            optimize("Gson_" + i, "com.google.gson.");
        }

        for (int i = 1; i <= 26; i++) {
            optimize("JacksonCore_" + i, "com.fasterxml.jackson.core.");
        }

        for (int i = 1; i <= 93; i++) {
            optimize("Jsoup_" + i, "org.jsoup.");
        }

        for (int i = 1; i <= 22; i++) {
            optimize("JxPath_" + i, "org.apache.commons.jxpath.");
        }

        for (int i = 1; i <= 64; i++) {
            if (i == 2) {
                continue;
            }
            optimize("Lang_" + i, "org.apache.commons.lang");
        }

        for (int i = 1; i <= 106; i++) {
            optimize("Math_" + i, "org.apache.commons.math");
        }

        for (int i = 1; i <= 38; i++) {
            try {
                optimize("Mockito_" + i, "org.mockito.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 1; i <= 27; i++) {
            if (i == 21) {
                continue;
            }
            optimize("Time_" + i, "org.joda.time.");
        }
    }

    public static void optimize(String bug, String sourcePrefix) throws Exception {
//        String bug = "Chart_1";
//        String sourcePrefix = "org.jfree.";
        System.out.println(bug);

        File flResult = new File("FL/original/GZoltar-1.7.2/" + bug + "/Ochiai.txt");
        File failingTestCases = new File("defects4j/failingtestcases/" + bug);
        File flResultOptimized = new File("FL/optimized/GZoltar-1.7.2/" + bug + "/Ochiai.txt");

        BufferedReader br = new BufferedReader(new FileReader(flResult));
        String buf;

        List<FLEntry> flEntries = new ArrayList<>();
        while ((buf = br.readLine()) != null) {
            String[] tokens = buf.split("@");
            FLEntry entry = new FLEntry(tokens[0], Integer.parseInt(tokens[1]), Double.parseDouble(tokens[2]));
            flEntries.add(entry);
        }
        br.close();

        if (flEntries.isEmpty()) {
            return;
        }

        br = new BufferedReader(new FileReader(failingTestCases));
        List<String> candidateClassName = new ArrayList<>();
        List<FLEntry> candidatePreciseLocation = new ArrayList<>();
        while ((buf = br.readLine()) != null) {
            String[] tokens = buf.trim().split(" ");
            if (tokens.length >= 2 &&
                    tokens[0].equals("at") &&
                    tokens[1].startsWith(sourcePrefix) &&
                    !tokens[1].contains("$")) {
                int preBracket = tokens[1].lastIndexOf('(');
                int postBracket = tokens[1].lastIndexOf(')');
                int dotJava = tokens[1].lastIndexOf('.');
                int lineNumberStart = tokens[1].lastIndexOf(':') + 1;
                String className = tokens[1].substring(preBracket + 1, dotJava);
                int lineNumber = Integer.parseInt(
                        tokens[1].substring(lineNumberStart, postBracket)
                );

                if (className.contains("Tests")) {
                    String n = className.replace("Tests", "");
                    if (!candidateClassName.contains(n)) {
                        candidateClassName.add(n);
                    }
                    continue;
                } else if (className.contains("Test")) {
                    String n = className.replace("Test", "");
                    if (!candidateClassName.contains(n)) {
                        candidateClassName.add(n);
                    }
                    continue;
                }
                if (className.contains("tests")) {
                    String n = className.replace("tests", "");
                    if (!candidateClassName.contains(n)) {
                        candidateClassName.add(n);
                    }
                    continue;
                } else if (className.contains("test")) {
                    String n = className.replace("test", "");
                    if (!candidateClassName.contains(n)) {
                        candidateClassName.add(n);
                    }
                    continue;
                }

                String wholeClassName = tokens[1].substring(0, tokens[1].indexOf(className) + className.length());
                FLEntry entry = new FLEntry(wholeClassName, lineNumber, 1d);
                if (!candidatePreciseLocation.contains(entry)) {
                    candidatePreciseLocation.add(entry);
                }
            }
        }
        br.close();

        List<FLEntry> prioritized = new ArrayList<>();
        for (String name : candidateClassName) {
            Iterator<FLEntry> iter = flEntries.iterator();
            while (iter.hasNext()) {
                FLEntry entry = iter.next();
                if (name.equals(entry.getSimpleClassName())) {
                    iter.remove();
                    entry.setSuspicious(0.999999d);
                    prioritized.add(entry);
                }
            }
        }
        flEntries.addAll(0, prioritized);

        prioritized = new ArrayList<>();
        for (int i = 0; i < candidatePreciseLocation.size(); i++) {
        // for (int i = candidatePreciseLocation.size() - 1; i >= 0; i--) {
            Iterator<FLEntry> iter = flEntries.iterator();
            while (iter.hasNext()) {
                FLEntry entry = iter.next();
                if (entry.getClassName().equals(candidatePreciseLocation.get(i).getClassName()) &&
                    entry.getLine() == candidatePreciseLocation.get(i).getLine()) {
                    iter.remove();
                }
            }

            prioritized.add(candidatePreciseLocation.get(i));
        }
        flEntries.addAll(0, prioritized);

        PrintWriter pw = new PrintWriter(flResultOptimized);
        for (FLEntry entry : flEntries) {
            pw.println(entry.getClassName() + "@" + entry.getLine() + "@" + entry.getSuspicious());
        }
        pw.close();

    }
}

class FLEntry {
    private String className;
    private int line;
    private double suspicious;

    public FLEntry(String className, int line, double suspicious) {
        this.className = className;
        this.line = line;
        this.suspicious = suspicious;
    }

    public double getSuspicious() {
        return suspicious;
    }

    public void setSuspicious(double suspicious) {
        this.suspicious = suspicious;
    }

    public int getLine() {
        return line;
    }

    public String getClassName() {
        return className;
    }

    public String getSimpleClassName() {
        String[] token = className.split("\\.");
        return token[token.length - 1];
    }

    @Override
    public boolean equals(Object obj) {
        FLEntry entry = (FLEntry) obj;
        return className.equals(entry.className) && line == entry.line && suspicious == entry.suspicious;
    }

    @Override
    public String toString() {
        return className + "@" + line + "@" + suspicious;
    }
}
