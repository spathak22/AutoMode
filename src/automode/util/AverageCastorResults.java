package automode.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by SherLock on 27/09/17.
 */
public class AverageCastorResults {
    List<Double> accuracy = new ArrayList<>();

    List<Double> precision = new ArrayList<>();

    List<Double> recall = new ArrayList<>();

    List<Double> f1 = new ArrayList<>();

    List<Integer> time = new ArrayList<>();

    public static void main(String[] args) {
        AverageCastorResults avg = new AverageCastorResults();
        File folder = new File("input/");
        File[] listOfFiles = folder.listFiles();

        System.out.println(" Processing " + listOfFiles.length + " Files \n");
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fname = (file.getName()).substring(0, (file.getName()).indexOf("."));
                //System.out.println(fname);
                avg.readFile(file.getPath());
            }
        }
        avg.printResults();
        //avg.writeToExcel();
    }

    private void readFile(String transformFile) {
        try {
            Files.lines(Paths.get(transformFile)).forEach(s -> processTransformationSchema(s));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void processTransformationSchema(String str) {
        String[] val = str.split(":");
        if (str.contains("Accuracy:")) {
            accuracy.add(Double.parseDouble(val[1]));
        }
        if (str.contains("Precision:")) {
            precision.add(Double.parseDouble(val[1]));
        }
        if (str.contains("Recall:")) {
            recall.add(Double.parseDouble(val[1]));
        }
        if (str.contains("F1:")) {
            f1.add(Double.parseDouble(val[1]));
        }
        if (str.contains("Elapsed time:")) {
            time.add(Integer.parseInt(val[1].trim()));
        }
    }

    public void printResults() {
        Double trainAccuracy = 0.0;
        Double testAccuracy = 0.0;
        for (int i = 0; i < accuracy.size(); i++) {
            if (i % 2 == 0) {
                trainAccuracy += accuracy.get(i);
            } else {
                testAccuracy += accuracy.get(i);
            }
        }

        Double trainPrec = 0.0;
        Double testPrec = 0.0;
        for (int i = 0; i < precision.size(); i++) {
            if (i % 2 == 0) {
                trainPrec += precision.get(i);
            } else {
                testPrec += precision.get(i);
            }
        }

        Double trainRecall = 0.0;
        Double testRecall = 0.0;
        for (int i = 0; i < recall.size(); i++) {
            if (i % 2 == 0) {
                trainRecall += recall.get(i);
            } else {
                testRecall += recall.get(i);
            }
        }

        Double trainF1 = 0.0;
        Double testF1 = 0.0;
        for (int i = 0; i < f1.size(); i++) {
            if (i % 2 == 0) {
                trainF1 += f1.get(i);
            } else {
                testF1 += f1.get(i);
            }
        }

        Double seconds = 0.0;
        for (Integer secs : time) {
            seconds = secs * 1.0;
        }
        seconds = seconds / time.size();


        System.out.println(" Train ");
        System.out.println(" Accuracy: " + trainAccuracy / (accuracy.size() / 2));
        System.out.println(" Precision: " + trainPrec / (precision.size() / 2));
        System.out.println(" Recall: " + trainRecall / (recall.size() / 2));
        System.out.println(" F1: " + trainF1 / (f1.size() / 2));

//        System.out.println("");

        System.out.println(" Test ");
        System.out.println(" Accuracy: " + testAccuracy / (accuracy.size() / 2));
        System.out.println(" Precision: " + testPrec / (precision.size() / 2));
        System.out.println(" Recall: " + testRecall / (recall.size() / 2));
        System.out.println(" F1: " + testF1 / (f1.size() / 2));

        if (seconds >= 60) {
            System.out.print(" Time: ");
            System.out.printf("%.1f", +seconds / 60);
            System.out.print("m");
        } else {
            System.out.println(" Time: " + seconds + "s");
        }
    }

}
