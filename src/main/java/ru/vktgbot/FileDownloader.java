package ru.vktgbot;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDownloader {
    private static Logger logger = LoggerFactory.getLogger(FileDownloader.class);

    public static List<byte[]> downloadFiles(List<String> urls) {
        return urls.parallelStream()
            .flatMap(url -> Stream.of(downloadFile(url)))
            .collect(Collectors.toList());
    }

    public static byte[] downloadFile(String urlLink) {
        try {
            URL url = new URL(urlLink);
            InputStream in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            
            for (int n = 0; (n = in.read(buf)) != -1;) {
                out.write(buf, 0, n);
            }
    
            out.close();
            in.close();
            return out.toByteArray();
        } catch (MalformedURLException e) {
            logger.error("invalid url link", e);
        } catch (IOException e) {
            logger.error("io exception", e);
        }

        return new byte[0];
    }
}
