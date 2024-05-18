package com.fsz._3Dcostestimator;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUploadUtil {
    //Later remember about deleting file.
    public static File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
        file.createNewFile();
        try (var fileOutputStream = Files.newOutputStream(file.toPath())) {
            fileOutputStream.write(multipartFile.getBytes());
        }
        return file;
    }
}