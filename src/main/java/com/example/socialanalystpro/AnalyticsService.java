package com.example.socialanalystpro;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface AnalyticsService extends Remote {
    // Changed 'int days' to 'long sinceTimestamp' for better precision (minutes/hours)
    SocialInsights analyzePage(String pageAccessToken, long sinceTimestamp) throws RemoteException;
    PageProfile getPageProfile(String pageAccessToken) throws RemoteException;
    String askViralBud(String prompt) throws RemoteException;
    long getFollowerCount(String pageAccessToken) throws RemoteException;
    long getPageLikes(String pageAccessToken) throws RemoteException;
    Map<String, Integer> getGenderBreakdown(String pageAccessToken) throws RemoteException;
    Map<String, Map<String, Integer>> getPostEngagementOverTime(String pageAccessToken, long sinceTimestamp) throws RemoteException;
    Map<String, Double> getBestDayToPost(String pageAccessToken, long sinceTimestamp) throws RemoteException;
    Map<String, Double> getBestHourToPost(String pageAccessToken, long sinceTimestamp) throws RemoteException;
    Map<String, Map<String, Integer>> getOverallPerformance(String pageAccessToken) throws RemoteException;
}
