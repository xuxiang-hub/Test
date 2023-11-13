package com.xuxiang.smartfast_rearend.test;

import com.theokanning.openai.completion.CompletionRequest;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class test {
    public static void main(String[] args) {
        // openAI api key
        String openAIkey = "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";
        String solcVersion = "0.7.29";
        System.out.println (isGreaterThan (solcVersion)); // true
    }

    public static boolean isGreaterThan (String v1) {
        ComparableVersion version1 = new ComparableVersion (v1);
        ComparableVersion version2 = new ComparableVersion ("0.8.0");
        return version1.compareTo (version2) > 0;
    }
}
