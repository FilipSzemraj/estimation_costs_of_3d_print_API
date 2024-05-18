package com.fsz._3Dcostestimator;

import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@SpringBootApplication
@RestController
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping("/root")
	public String apiRoot(){
		return "Hello world!";
	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
		try {
			File tempFile = FileUploadUtil.convertToFile(file, "tempFile.stl");
			STLReader stlReader = new STLReader(tempFile);
			return ResponseEntity.ok("File uploaded successfully: " + file.getOriginalFilename());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload the file: " + file.getOriginalFilename() + "!");
		}
	}

}
