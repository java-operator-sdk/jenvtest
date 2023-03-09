package com.csviri.jenvtest;

import org.slf4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Utils {

    private Utils() {
    }

    public static final SemverComparator SEMVER_COMPARATOR = new SemverComparator();

    public static String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else {
            return os;
        }
    }

    public static String getOSArch() {
        return System.getProperty("os.arch").toLowerCase();
    }

    public static String getLatestVersion(List<String> versions) {
        versions.sort(SEMVER_COMPARATOR);
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

    public static void redirectProcessOutputToLogger(InputStream outputStream, Logger logger) {
        new Thread(() -> {
            Scanner sc = new Scanner(outputStream);
            while (sc.hasNextLine()) {
                logger.trace( sc.nextLine());
            }
        }).start();
    }

}
