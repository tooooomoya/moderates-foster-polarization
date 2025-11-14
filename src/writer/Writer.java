package writer;

import agent.*;
import constants.Const;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Writer {
    private int simulationStep;
    private String folderPath;
    private double opinionVar;
    private double postOpinionVar;
    private int followActionNum;
    private int unfollowActionNum;
    private String[] resultList;
    private int rewireActionNum;
    private int[] postBins = new int[Const.NUM_OF_BINS_OF_POSTS];
    private double postBinWidth;
    private int[] opinionBins = new int[Const.NUM_OF_BINS_OF_OPINION_FOR_WRITER];
    private double opinionBinWidth;
    private double opinionAvg;
    private double feedVar;
    private double[] feedMeanArray;
    private double[] feedVarArray;
    private double[] cRateMeanArray;
    private double[] cRateVarArray;
    private double[] highComfortRateNumArray;

    public Writer(String folderPath, String[] resultList) {
        this.simulationStep = -1;
        this.folderPath = folderPath;
        this.opinionVar = -1;
        this.postOpinionVar = -1;
        this.followActionNum = 0;
        this.unfollowActionNum = 0;
        this.rewireActionNum = 0;
        this.resultList = resultList;
        this.postBinWidth = 2.0 / postBins.length;
        this.opinionBinWidth = 2.0 / opinionBins.length;
        this.opinionAvg = 0.0;
        this.feedVar = -1;
        this.feedMeanArray = new double[Const.NUM_OF_BINS_OF_OPINION];
        this.feedVarArray = new double[Const.NUM_OF_BINS_OF_OPINION];
        this.cRateMeanArray = new double[Const.NUM_OF_BINS_OF_OPINION];
        this.cRateVarArray = new double[Const.NUM_OF_BINS_OF_OPINION];
        this.highComfortRateNumArray = new double[Const.NUM_OF_BINS_OF_OPINION];
    }

    // Setter
    public void setSimulationStep(int step) {
        this.simulationStep = step;
        this.opinionVar = -1;
    }

    public void setOpinionVar(double var) {
        this.opinionVar = var;
    }

    public void setPostOpinionVar(double var) {
        this.postOpinionVar = var;
    }

    public void setRewireActionNum(int value) {
        this.rewireActionNum = value;
    }

    public void setFollowUnfollowActionNum(int followActionNum, int unfollowActionNum) {
        this.followActionNum = followActionNum;
        this.unfollowActionNum = unfollowActionNum;
    }

    public void setOpinionAvg(double value) {
        this.opinionAvg = value;
    }

    public void setFeedVar(double value) {
        this.feedVar = value;
    }

    public void setFeedMeanArray(double[] original) {
        this.feedMeanArray = original.clone();
    }

    public void setFeedVarArray(double[] original) {
        this.feedVarArray = original.clone();
    }

    public void setCRateMeanArray(double[] original) {
        this.cRateMeanArray = original.clone();
    }

    public void setCRateVarArray(double[] original) {
        this.cRateVarArray = original.clone();
    }

    public void setHighComfortRateNumArray(double[] original) {
        this.highComfortRateNumArray = original.clone();
    }

    public void clearPostBins() {
        for (int i = 0; i < postBins.length; i++) {
            postBins[i] = 0;
        }
    }

    public void setPostBins(Post post) {
        double shiftedOpinion = post.getPostOpinion() + 1;
        int binIndex = (int) Math.min(shiftedOpinion / postBinWidth, postBins.length - 1);
        postBins[binIndex] += 1;
    }

    public void setOpinionBins(Agent[] agentSet) {
        double opinionBinWidth = 2.0 / Const.NUM_OF_BINS_OF_OPINION_FOR_WRITER;
        for (int i = 0; i < opinionBins.length; i++) {
            opinionBins[i] = 0;
        }
        for (Agent agent : agentSet) {
            if (!agent.getTarget()) {
                double shiftedOpinion = agent.getOpinion() + 1; // [-1,1] → [0,2]
                int opinionClass = (int) Math.min(shiftedOpinion / opinionBinWidth,
                        Const.NUM_OF_BINS_OF_OPINION_FOR_WRITER - 1);
                this.opinionBins[opinionClass] += 1;
            }
        }
    }

    public void write() {

        //// metrics
        String filePath = folderPath + "/metrics/result_" + simulationStep + ".csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) { // ← false で上書きモード
            // write a header row
            StringBuilder header = new StringBuilder();
            header.append("step");
            for (String key : resultList) {
                header.append(",").append(key);
            }
            pw.println(header.toString());

            // write a result raw
            StringBuilder sb = new StringBuilder();
            sb.append(simulationStep);

            for (String key : resultList) {
                sb.append(",");
                switch (key) {
                    case "opinionVar":
                        sb.append(String.format("%.4f", this.opinionVar));
                        break;
                    case "postOpinionVar":
                        sb.append(String.format("%.4f", this.postOpinionVar));
                        break;
                    case "follow":
                        sb.append(this.followActionNum);
                        break;
                    case "unfollow":
                        sb.append(this.unfollowActionNum);
                        break;
                    case "rewire":
                        sb.append(this.rewireActionNum);
                        break;
                    case "opinionAvg":
                        sb.append(String.format("%.4f", this.opinionAvg));
                        break;
                    case "feedVar":
                        sb.append(String.format("%.4f", this.feedVar));
                        break;
                    case "feedPostOpinionMean_0":
                        sb.append(String.format("%.4f", this.feedMeanArray[0]));
                        break;
                    case "feedPostOpinionMean_1":
                        sb.append(String.format("%.4f", this.feedMeanArray[1]));
                        break;
                    case "feedPostOpinionMean_2":
                        sb.append(String.format("%.4f", this.feedMeanArray[2]));
                        break;
                    case "feedPostOpinionMean_3":
                        sb.append(String.format("%.4f", this.feedMeanArray[3]));
                        break;
                    case "feedPostOpinionMean_4":
                        sb.append(String.format("%.4f", this.feedMeanArray[4]));
                        break;
                    case "feedPostOpinionVar_0":
                        sb.append(String.format("%.4f", this.feedVarArray[0]));
                        break;
                    case "feedPostOpinionVar_1":
                        sb.append(String.format("%.4f", this.feedVarArray[1]));
                        break;
                    case "feedPostOpinionVar_2":
                        sb.append(String.format("%.4f", this.feedVarArray[2]));
                        break;
                    case "feedPostOpinionVar_3":
                        sb.append(String.format("%.4f", this.feedVarArray[3]));
                        break;
                    case "feedPostOpinionVar_4":
                        sb.append(String.format("%.4f", this.feedVarArray[4]));
                        break;
                    case "cRateMean_0":
                        sb.append(String.format("%.4f", this.cRateMeanArray[0]));
                        break;
                    case "cRateMean_1":
                        sb.append(String.format("%.4f", this.cRateMeanArray[1]));
                        break;
                    case "cRateMean_2":
                        sb.append(String.format("%.4f", this.cRateMeanArray[2]));
                        break;
                    case "cRateMean_3":
                        sb.append(String.format("%.4f", this.cRateMeanArray[3]));
                        break;
                    case "cRateMean_4":
                        sb.append(String.format("%.4f", this.cRateMeanArray[4]));
                        break;
                    case "cRateVar_0":
                        sb.append(String.format("%.4f", this.cRateVarArray[0]));
                        break;
                    case "cRateVar_1":
                        sb.append(String.format("%.4f", this.cRateVarArray[1]));
                        break;
                    case "cRateVar_2":
                        sb.append(String.format("%.4f", this.cRateVarArray[2]));
                        break;
                    case "cRateVar_3":
                        sb.append(String.format("%.4f", this.cRateVarArray[3]));
                        break;
                    case "cRateVar_4":
                        sb.append(String.format("%.4f", this.cRateVarArray[4]));
                        break;
                    case "highComfortRateNum_0":
                        sb.append(String.format("%.4f", this.highComfortRateNumArray[0]));
                        break;
                    case "highComfortRateNum_1":
                        sb.append(String.format("%.4f", this.highComfortRateNumArray[1]));
                        break;
                    case "highComfortRateNum_2":
                        sb.append(String.format("%.4f", this.highComfortRateNumArray[2]));
                        break;
                    case "highComfortRateNum_3":    
                        sb.append(String.format("%.4f", this.highComfortRateNumArray[3]));
                        break;
                    case "highComfortRateNum_4":
                        sb.append(String.format("%.4f", this.highComfortRateNumArray[4]));
                        break;     

                    default:
                        sb.append(""); // for undefined elements
                }
            }

            pw.println(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //// posts related metrics
        String postsFilePath = folderPath + "/posts/post_result_" + simulationStep + ".csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(postsFilePath, false))) {
            StringBuilder header = new StringBuilder();
            header.append("step");

            for (int i = 0; i < postBins.length; i++) {
                header.append(",bin_").append(i);
            }
            header.append(",sumOfPosts");
            pw.println(header.toString());

            StringBuilder sb = new StringBuilder();
            sb.append(simulationStep);
            int sumOfPosts = 0;

            for (int i = 0; i < postBins.length; i++) {
                sb.append(",").append(postBins[i]);
                sumOfPosts += postBins[i];
            }
            sb.append(",").append(sumOfPosts);

            pw.println(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //// opinion related metrics
        String opinionFilePath = folderPath + "/opinion/opinion_result_" + simulationStep + ".csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(opinionFilePath, false))) {

            StringBuilder header = new StringBuilder();
            header.append("step");

            for (int i = 0; i < opinionBins.length; i++) {
                header.append(",bin_").append(i);
            }
            pw.println(header.toString());

            StringBuilder sb = new StringBuilder();
            sb.append(simulationStep);

            for (int i = 0; i < opinionBins.length; i++) {
                sb.append(",").append(opinionBins[i]);
            }

            pw.println(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeDegrees(double[][] adjacencyMatrix, String outputDirPath) {
        String filePath = outputDirPath + "/degrees/degree_result_" + simulationStep + ".csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {

            pw.println("agentId,inDegree,outDegree");

            int numAgents = adjacencyMatrix.length;

            for (int i = 0; i < numAgents; i++) {
                int outDegree = 0;
                int inDegree = 0;

                for (int j = 0; j < numAgents; j++) {
                    outDegree += (adjacencyMatrix[i][j] > 0) ? 1 : 0;
                }

                for (int j = 0; j < numAgents; j++) {
                    inDegree += (adjacencyMatrix[j][i] > 0) ? 1 : 0;
                }

                pw.printf("%d,%d,%d%n", i, inDegree, outDegree);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeClusteringCoefficients(double[] clustering, String outputDirPath) {
        String filePath = outputDirPath + "/clusterings/clustering_result_" + simulationStep + ".csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {

            pw.println("agentId,clusteringCoefficient");

            for (int i = 0; i < clustering.length; i++) {
                pw.printf("%d,%.6f%n", i, clustering[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}