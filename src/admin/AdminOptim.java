package admin;

import agent.*;
import constants.Const;
import java.util.*;
import rand.randomGenerator;

public class AdminOptim {
    private int n;
    private Set<Integer>[] followees;  // followees[i] = set of agents that i follows
    private Set<Integer>[] followers;  // followers[j] = set of agents that follow j
    private int[] followerNumArray;
    private List<Post> recommendPostQueue = new ArrayList<>();
    private int maxRecommPostQueueLength = Const.MAX_RECOMMENDATION_POST_LENGTH;

    @SuppressWarnings("unchecked")
    public AdminOptim(int userNum, double[][] W) {
        this.n = userNum;
        this.followerNumArray = new int[n];
        this.followees = new HashSet[n];
        this.followers = new HashSet[n];
        for (int i = 0; i < n; i++) {
            followees[i] = new HashSet<>();
            followers[i] = new HashSet<>();
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (W[i][j] > 0.0) {
                    followees[i].add(j);
                    followers[j].add(i);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            followerNumArray[i] = followers[i].size();
        }
    }

    /** Generate adjacency matrix on demand (only for GEXF export / degree / clustering, every 5000 steps). */
    public double[][] getAdjacencyMatrix() {
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j : followees[i]) {
                matrix[i][j] = 1.0;
            }
        }
        return matrix;
    }

    public Set<Integer> getFollowers(int userId) {
        return followers[userId];
    }

    public Set<Integer> getFollowees(int userId) {
        return followees[userId];
    }

    public int[] getFollowerList() {
        return this.followerNumArray.clone();
    }

    public List<Map.Entry<Integer, Integer>> getFollowerRanking() {
        List<Map.Entry<Integer, Integer>> rankingList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rankingList.add(new AbstractMap.SimpleEntry<>(i, followerNumArray[i]));
        }
        rankingList.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return rankingList;
    }

    public void updateAdjacencyMatrix(int userId, int[] followedIds, int unfollowedId) {
        if (followedIds[0] >= 0) {
            followees[userId].add(followedIds[0]);
            followers[followedIds[0]].add(userId);
            followerNumArray[followedIds[0]]++;
        }
        if (followedIds[1] >= 0) {
            followees[userId].remove(followedIds[1]);
            followers[followedIds[1]].remove(userId);
            followerNumArray[followedIds[1]]--;
        }
        if (unfollowedId >= 0) {
            followees[userId].remove(unfollowedId);
            followers[unfollowedId].remove(userId);
            followerNumArray[unfollowedId]--;
        }
    }

    /** Compute weighted disagreement using adjacency lists (O(edges) per step). */
    public double computeDisagreement(Agent[] agentSet) {
        double sum = 0.0;
        int edgeCount = 0;
        for (int i = 0; i < n; i++) {
            for (int j : followees[i]) {
                double diff = agentSet[i].getOpinion() - agentSet[j].getOpinion();
                sum += diff * diff;
                edgeCount++;
            }
        }
        return edgeCount > 0 ? sum / edgeCount : 0.0;
    }

    public void AdminFeedback(int userId, Agent[] agentSet) {
        List<Post> tempFeed = new ArrayList<>();
        for (Post post : agentSet[userId].getPostCash().getAllPosts()) {
            if (!agentSet[userId].getUnfollowList()[post.getPostUserId()]) {
                tempFeed.add(post);
            }
        }
        Collections.shuffle(tempFeed, randomGenerator.get());
        for (Post post : tempFeed) {
            agentSet[userId].addPostToFeed(post);
        }
    }

    public List<Integer> getManipulationTarget(Agent[] agentSet) {
        List<Map.Entry<Integer, Integer>> rankingList = getFollowerRanking();
        List<Integer> neutralUsers = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : rankingList) {
            int userId = entry.getKey();
            double opinion = agentSet[userId].getOpinion();
            if (Math.abs(opinion) < 0.2) {
                neutralUsers.add(userId);
            }
        }
        List<Integer> result = new ArrayList<>();
        if (neutralUsers.size() > 2) result.add(neutralUsers.get(1));
        if (neutralUsers.size() > 3) result.add(neutralUsers.get(2));

        for (int id : result) {
            System.out.println("Manipulation Target User ID: " + id
                    + ", Opinion: " + agentSet[id].getOpinion()
                    + ", Followers: " + followerNumArray[id]);
        }
        for (int id : result) {
            if (followerNumArray[id] < Const.FOLLOWER_THRESHOLD) {
                System.out.println("⚠️ [TERMINATE] Target User " + id + " has only "
                        + followerNumArray[id] + " followers (Threshold: " + Const.FOLLOWER_THRESHOLD + ")");
                System.exit(0);
            }
        }
        return result;
    }

    public List<Integer> getTopInfluencers(int topK) {
        for (int i = 0; i < n; i++) {
            followerNumArray[i] = followers[i].size();
        }
        List<Map.Entry<Integer, Integer>> rankingList = getFollowerRanking();
        List<Integer> topInfluencers = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, rankingList.size()); i++) {
            topInfluencers.add(rankingList.get(i).getKey());
        }
        return topInfluencers;
    }
}
