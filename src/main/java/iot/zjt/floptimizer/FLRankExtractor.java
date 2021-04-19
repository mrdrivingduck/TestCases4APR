package iot.zjt.floptimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FLRankExtractor {
    private static Map<String, Map<String, Set<Integer>>> realBugPosition;

    private static void initRealBugPosition() {
        realBugPosition = new HashMap<>();

        File bugPositionFile = new File("defects4j/D4jPatchPosition.txt");
        if (!bugPositionFile.exists()) {
            return;
        }

        BufferedReader reader = null;
        String line = null;
        int lineCounter = 0;
        try {
            reader = new BufferedReader(new FileReader(bugPositionFile));

            while ((line = reader.readLine()) != null) {
                String[] splits = line.split("@");
                lineCounter++;

                if (splits.length != 3) {
//                    System.out.println("Empty buggy line at line " + lineCounter);
                    continue;
                }

                String bug = splits[0];
                Map<String, Set<Integer>> buggyClasses = realBugPosition.computeIfAbsent(bug, k -> new HashMap<>());

                String className = splits[1].replace('/', '.');
                className = className.substring(0, className.length() - ".java".length());
                Set<Integer> buggyLines = buggyClasses.computeIfAbsent(className, k -> new HashSet<>());

                String[] positions = splits[2].split(",");
                for (int i = 0; i < positions.length; i++) {
                    if (positions[i].contains("-")) {
                        String[] minAndMax = positions[i].split("-");
                        if (minAndMax.length != 2) {
                            System.err.println("Error buggy line format.");
                            break;
                        }
                        int min = Integer.parseInt(minAndMax[0]);
                        int max = Integer.parseInt(minAndMax[1]);
                        for (int j = min; j <= max; j++) {
                            buggyLines.add(j);
                        }
                    } else {
                        buggyLines.add(Integer.valueOf(positions[i]));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(lineCounter);
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        initRealBugPosition();

        File suspiciousCodePositions = new File("FL/original/GZoltar-1.7.2/");
        if (!suspiciousCodePositions.exists()) {
            return;
        }

        File[] bugFileDir = suspiciousCodePositions.listFiles();
        assert bugFileDir != null;
        Arrays.sort(bugFileDir, (o1, o2) -> {
            String[] f1 = o1.getName().split("_");
            String[] f2 = o2.getName().split("_");

            if (f1[0].equals(f2[0])) {
                return Integer.parseInt(f1[1]) - Integer.parseInt(f2[1]);
            } else {
                return f1[0].compareTo(f2[0]);
            }
        });

        for (File bug : bugFileDir) {
            if (!bug.getName().startsWith("Chart_")) {
                continue;
            }
            if (!bug.getName().startsWith("Cli_")) {
                continue;
            }
            if (!bug.getName().startsWith("Closure") || Integer.valueOf(bug.getName().split("_")[1]) >= 134) {
                continue;
            }
            if (!bug.getName().startsWith("Codec_")) {
                continue;
            }
            if (!bug.getName().startsWith("Collections_")) {
                continue;
            }
            if (!bug.getName().startsWith("Compress_")) {
                continue;
            }
            if (!bug.getName().startsWith("Csv_")) {
                continue;
            }
            if (!bug.getName().startsWith("Gson_")) {
                continue;
            }
            if (!bug.getName().startsWith("JacksonCore_")) {
                continue;
            }
            if (!bug.getName().startsWith("JacksonDatabind_")) {
                continue;
            }
            if (!bug.getName().startsWith("JacksonXml_")) {
                continue;
            }
            if (!bug.getName().startsWith("Jsoup_")) {
                continue;
            }
            if (!bug.getName().startsWith("JxPath_")) {
                continue;
            }
            if (!bug.getName().startsWith("Lang")) {
                continue;
            }
            if (!bug.getName().startsWith("Math")) {
                continue;
            }
            if (!bug.getName().startsWith("Mockito")) {
                continue;
            }
            if (!bug.getName().startsWith("Time")) {
                continue;
            }

            // System.out.println(bug.getName());

            File flFile = new File(bug, "Ochiai.txt");
            BufferedReader reader = null;
            Map<String, Set<Integer>> buggyClasses = realBugPosition.get(bug.getName()); // class name
            if (buggyClasses == null) {
                System.out.println("-");
                continue;
            }

            try {
                reader = new BufferedReader(new FileReader(flFile));
                String line = null;
                int lineCount = 0;
                boolean found = false;

                iterBugFile:
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    String[] splits = line.split("@");

                    for (String key : buggyClasses.keySet()) {
                        if (key.contains(splits[0])) {
                            Set<Integer> linesInFile = buggyClasses.get(key);
                            if (linesInFile.contains(Integer.valueOf(splits[1]))) {
                                // System.out.println(lineCount + " " + bug.getName() + " " + splits[0]);
                                System.out.println(lineCount);
                                found = true;
                                break iterBugFile;
                            }
                        }
                    }
                }
                if (!found) {
                    System.out.println("-");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
