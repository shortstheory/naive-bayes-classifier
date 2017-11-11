import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by nic on 7/11/17.
 */

public class NaiveBayesClassifier {
    final private static String testFolderPath = "/home/nic/original-projects/ml3/aclImdb/";
    final private static String testLabel = "test/labeledBow.feat";
    final private static String imdbVocab = "imdb.vocab";
    ArrayList<FileRating> fileRatingArrayList;

    float positive = 0;
    float negative = 0;
    float true_positive = 0;
    float true_negative = 0;
    float false_positive = 0;
    float false_negative = 0;

    private FileParse fileParse;

    boolean binaryClassifier;

    NaiveBayesClassifier(boolean isBinaryClassifier, boolean removeStopWords) {
        binaryClassifier = isBinaryClassifier;
        fileParse = new FileParse();

        if (removeStopWords) {
            fileParse.removeStopWords();
        }

        String testPath = testFolderPath + testLabel;

        try {
            FileInputStream fileInputStream = new FileInputStream(testPath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            fileRatingArrayList = new ArrayList<FileRating>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] words = line.split("\\s+");
                FileRating fileRating = new FileRating();
                fileRating.movieRating = Integer.parseInt(words[0]);
                for (int i = 1; i < words.length; i++) {
                    String[] keyValue = words[i].split(":");

                    int key = Integer.parseInt(keyValue[0]); //wordID
                    int value = Integer.parseInt(keyValue[1]); //value
                    fileRating.wordMap.put(key, value);
                }
                fileRatingArrayList.add(fileRating);
            }
        } catch (Exception e) {
            System.out.println("Exception in: " + e.getMessage());
        }
    }

    public String classifyInstance(int i) { //gets file i
        float posProbability = 0;
        float negProbability = 0;
        int scoredRating = fileRatingArrayList.get(i).movieRating;

        boolean actuallyGoodFilm;

        if (scoredRating >= 7) {
            actuallyGoodFilm = true;
        } else {
            actuallyGoodFilm = false;
        }

        HashMap<Integer, Integer> map = fileRatingArrayList.get(i).wordMap; //first arg is the index of the word, second is # of times it occurs
        Iterator it = map.entrySet().iterator();

        while (it.hasNext()) { //iterate through all the words in the wordmap
            HashMap.Entry<Integer, Integer> pair = (HashMap.Entry<Integer, Integer>) it.next();

            Integer key = (Integer) pair.getKey();
            Integer val = (Integer) pair.getValue(); //number of occ of word in files
            //search for key in the global map
            FileParse.PosNegPair posNegPair = fileParse.globalMap.get(key);
            if (posNegPair != null) {
                int posOccurences = posNegPair.posOccurences;
                int negOccurences = posNegPair.negOccurences;
                if (binaryClassifier) {
                    posProbability += Math.log((double) (posOccurences + 1) / (fileParse.posWords + fileParse.totalWords));
                    negProbability += Math.log((double) (negOccurences + 1) / (fileParse.negWords + fileParse.totalWords));
                } else {
                    posProbability += val * Math.log((double) (posOccurences + 1) / (fileParse.posWords + fileParse.totalWords));
                    negProbability += val * Math.log((double) (negOccurences + 1) / (fileParse.negWords + fileParse.totalWords));
                }
            }
            it.remove();
        }
        posProbability += fileParse.priorPosLog;
        negProbability += fileParse.priorNegLog;
        boolean goodFilm;

        if (posProbability > negProbability) {
            goodFilm = true;
        } else {
            goodFilm = false;
        }

        if (actuallyGoodFilm && goodFilm) {
            positive++;
            true_positive++;
        } else if (actuallyGoodFilm && !goodFilm) {
            positive++;
            false_negative++;
        } else if (!actuallyGoodFilm && !goodFilm) {
            negative++;
            true_negative++;
        } else if (!actuallyGoodFilm && goodFilm) {
            negative++;
            false_positive++;
        }

        if (goodFilm) {
            return "Good movie!";
        } else {
            return "Bad movie!";
        }
    }

    public void printResults() {
        float accuracy = (true_positive+true_negative) / (true_positive + false_negative + false_positive + true_negative);

        float posPrecision = true_positive / (true_positive + false_positive);
        float posRecall = true_positive / (true_positive + false_negative);
        float posf1_score = 2 * (posPrecision*posRecall) / (posPrecision+posRecall);

        float negPrecision = true_negative / (true_negative + false_negative);
        float negRecall = true_negative / (true_negative + false_positive);
        float negf1_score = 2 * (negPrecision*negRecall) / (negPrecision+negRecall);

        System.out.println("Positive precision: " + posPrecision);
        System.out.println("Positive recall: " + posRecall);
        System.out.println("Positive F1 score: " + posf1_score);

        System.out.println("Negative precision: " + negPrecision);
        System.out.println("Negative recall: " + negRecall);
        System.out.println("Negative F1 score: " + negf1_score);
        System.out.println("Accuracy: " + accuracy);
    }

    public static void main(String[] args) {
        System.out.println("Standard Naive Bayes Classifier\n-----------------");
        NaiveBayesClassifier naiveBayesClassifier = new NaiveBayesClassifier(false, false);
        for (int i = 0; i < naiveBayesClassifier.fileRatingArrayList.size(); i++) {
            naiveBayesClassifier.classifyInstance(i);
        }
        naiveBayesClassifier.printResults();
        System.out.println("\nBinary Naive Bayes Classifier\n-----------------");
        NaiveBayesClassifier binaryNaiveBayesClassifier = new NaiveBayesClassifier(true, false);
        for (int i = 0; i < binaryNaiveBayesClassifier.fileRatingArrayList.size(); i++) {
            binaryNaiveBayesClassifier.classifyInstance(i);
        }
        binaryNaiveBayesClassifier.printResults();
        System.out.println("\nNaive Bayes Classifier With Stop Words Removed\n-----------------");
        NaiveBayesClassifier stopWordsNaiveBayesClassifier = new NaiveBayesClassifier(false, true);
        for (int i = 0; i < stopWordsNaiveBayesClassifier.fileRatingArrayList.size(); i++) {
            stopWordsNaiveBayesClassifier.classifyInstance(i);
        }
        stopWordsNaiveBayesClassifier.printResults();
    }
}
