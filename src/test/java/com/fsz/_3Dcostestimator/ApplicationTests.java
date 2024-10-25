package com.fsz._3Dcostestimator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;



@SpringBootTest
class ApplicationTests {
	@Test
	void testFileProcessing(){
		Instant start = Instant.now();

		assertTimeout(Duration.ofMillis(1500), () -> {
		Path path = Path.of("src/main/resources/astronaut.stl");
		byte[] content = Files.readAllBytes(path);
		MockMultipartFile mockFile = new MockMultipartFile("file", "test.stl", "application/octet-stream", content);

		Application controller = new Application();
		ResponseEntity<?> response = controller.handleReceivingForm(mockFile, 1, 20, 0.05, "mm", "without post-processing", "standard", (short) 1, "high");

		assertTrue(response.getStatusCode().is2xxSuccessful());

		CalculationResult result = (CalculationResult) response.getBody();
		System.out.println(result);
		assertEquals(67.09, result.surfaceArea(), 0.01);
		assertEquals(6.547, result.weight(), 0.01);
		assertEquals(18.057, result.volume(), 0.01);
		//assertEquals(7.357, result.price(), 0.01);
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		System.out.println("Test duration: " + timeElapsed + " ms");

	}

	@Test
	void testGettingMaterialsList(){

	}

}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MaterialControllerTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testGettingMaterialsList() {
		ParameterizedTypeReference<Map<Integer, Map<String, Double>>> responseType =
				new ParameterizedTypeReference<>() {
				};

		ResponseEntity<Map<Integer, Map<String, Double>>> response =
				restTemplate.exchange("/materials/list", HttpMethod.GET, HttpEntity.EMPTY, responseType);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		assertTrue(response.getBody().containsKey(1));
		assertNotNull(response.getBody().get(1));

		assertTrue(response.getBody().containsKey(2));
		assertNotNull(response.getBody().get(2));

		assertEquals(response.getBody().get(1).toString(), "{ABS=1.04}");
	}
}
