package com.konsel.genelab.bio;

import com.konsel.genelab.model.AlignmentResult;

public class AlignmentService {

    public static AlignmentResult needlemanWunsch(String a, String b, int match, int mismatch, int gap) {
        if (a == null)
            a = "";
        if (b == null)
            b = "";
        a = a.toUpperCase();
        b = b.toUpperCase();
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++)
            dp[i][0] = dp[i - 1][0] + gap;
        for (int j = 1; j <= m; j++)
            dp[0][j] = dp[0][j - 1] + gap;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diag = dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch);
                int up = dp[i - 1][j] + gap;
                int left = dp[i][j - 1] + gap;
                dp[i][j] = Math.max(diag, Math.max(up, left));
            }
        }
        // backtrack
        StringBuilder aa = new StringBuilder(), bb = new StringBuilder(), mid = new StringBuilder();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0
                    && dp[i][j] == dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch)) {
                char ca = a.charAt(i - 1), cb = b.charAt(j - 1);
                aa.append(ca);
                bb.append(cb);
                mid.append(ca == cb ? '|' : ' ');
                i--;
                j--;
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + gap) {
                aa.append(a.charAt(i - 1));
                bb.append('-');
                mid.append(' ');
                i--;
            } else {
                aa.append('-');
                bb.append(b.charAt(j - 1));
                mid.append(' ');
                j--;
            }
        }
        return new AlignmentResult(dp[n][m], aa.reverse().toString(), bb.reverse().toString(),
                mid.reverse().toString());
    }

    public static AlignmentResult smithWaterman(String a, String b, int match, int mismatch, int gap) {
        if (a == null)
            a = "";
        if (b == null)
            b = "";
        a = a.toUpperCase();
        b = b.toUpperCase();
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        int best = 0, bi = 0, bj = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diag = dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch);
                int up = dp[i - 1][j] + gap;
                int left = dp[i][j - 1] + gap;
                dp[i][j] = Math.max(0, Math.max(diag, Math.max(up, left)));
                if (dp[i][j] > best) {
                    best = dp[i][j];
                    bi = i;
                    bj = j;
                }
            }
        }
        StringBuilder aa = new StringBuilder(), bb = new StringBuilder(), mid = new StringBuilder();
        int i = bi, j = bj;
        while (i > 0 && j > 0 && dp[i][j] > 0) {
            int score = dp[i][j];
            if (score == dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch)) {
                char ca = a.charAt(i - 1), cb = b.charAt(j - 1);
                aa.append(ca);
                bb.append(cb);
                mid.append(ca == cb ? '|' : ' ');
                i--;
                j--;
            } else if (score == dp[i - 1][j] + gap) {
                aa.append(a.charAt(i - 1));
                bb.append('-');
                mid.append(' ');
                i--;
            } else {
                aa.append('-');
                bb.append(b.charAt(j - 1));
                mid.append(' ');
                j--;
            }
        }
        return new AlignmentResult(best, aa.reverse().toString(), bb.reverse().toString(), mid.reverse().toString());
    }
}
