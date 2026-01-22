package net.statemesh.service.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3EndpointParser {
    private static final Pattern AWS_PATTERN = Pattern.compile("(.*?)\\.s3[.-]([\\w-]+)\\.");
    private static final Pattern AZURE_PATTERN = Pattern.compile("(.*?)\\.blob\\.core\\.windows\\.net");
    private static final Pattern GCP_PATTERN = Pattern.compile("(.*?)\\.storage\\.googleapis\\.com");
    private static final Pattern CUSTOM_PATTERN = Pattern.compile("(https?://[^/]+)");

    public static S3Config parseEndpoint(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        if (url.contains("amazonaws.com")) {
            return parseAWS(url);
        } else if (url.contains("windows.net")) {
            return parseAzure(url);
        } else if (url.contains("googleapis.com")) {
            return parseGCP(url);
        }
        return parseCustom(url);
    }

    private static S3Config parseAWS(String url) {
        Matcher matcher = AWS_PATTERN.matcher(url);
        if (matcher.find()) {
            String region = matcher.group(2);
            return new S3Config(
                "https://s3." + region + ".amazonaws.com",
                region,
                StorageProvider.AWS
            );
        }
        throw new IllegalArgumentException("Invalid AWS URL format");
    }

    private static S3Config parseAzure(String url) {
        Matcher matcher = AZURE_PATTERN.matcher(url);
        if (matcher.find()) {
            return new S3Config(
                "https://blob.core.windows.net",
                "default",
                StorageProvider.AZURE
            );
        }
        throw new IllegalArgumentException("Invalid Azure URL format");
    }

    private static S3Config parseGCP(String url) {
        Matcher matcher = GCP_PATTERN.matcher(url);
        if (matcher.find()) {
            return new S3Config(
                "https://storage.googleapis.com",
                "auto",
                StorageProvider.GCP
            );
        }
        throw new IllegalArgumentException("Invalid GCP URL format");
    }

    private static S3Config parseCustom(String url) {
        Matcher matcher = CUSTOM_PATTERN.matcher(url);
        if (matcher.find()) {
            String endpoint = matcher.group(1);
            return new S3Config(
                endpoint,
                "us-east-1",
                StorageProvider.CUSTOM
            );
        }
        throw new IllegalArgumentException("Invalid custom URL format");
    }
}

