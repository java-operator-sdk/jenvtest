package com.csviri.kubeapi;

import java.util.Comparator;
import java.util.List;

public class SemverUtil {

    private static SemverComparator semverComparator = new SemverComparator();


    public static final String getLatestVersion(List<String> versions) {
        versions.sort(semverComparator);
        return versions.get(versions.size()-1);
    }

    private static class SemverComparator implements Comparator<String> {
        @Override
        public int compare(String v1, String v2) {
            String[] thisParts = v1.split("\\.");
            String[] thatParts = v2.split("\\.");
            int length = Math.max(thisParts.length, thatParts.length);
            for(int i = 0; i < length; i++) {
                int thisPart = i < thisParts.length ?
                        Integer.parseInt(thisParts[i]) : 0;
                int thatPart = i < thatParts.length ?
                        Integer.parseInt(thatParts[i]) : 0;
                if(thisPart < thatPart)
                    return -1;
                if(thisPart > thatPart)
                    return 1;
            }
            return 0;
        }
    }

}
