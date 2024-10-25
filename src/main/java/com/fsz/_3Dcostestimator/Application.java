package com.fsz._3Dcostestimator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@SpringBootApplication
@RestController
@RequestMapping("/api")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	@CrossOrigin(origins = {"http://localhost:80"})
	@PostMapping("/upload")
	public ResponseEntity<?> handleReceivingForm(@RequestParam("file") MultipartFile file,
												 	@RequestParam("materialId") int materialId,
												 	@RequestParam("infillPercentage") int infillPercentage,
												 	@RequestParam("surfaceThickness") double surfaceThickness,
												 	@RequestParam("unit") String unit,
												 	@RequestParam("postProcessing") String postProcessing,
												 	@RequestParam("executionTime") String executionTime,
												 	@RequestParam("quantity") short quantity,
												 	@RequestParam("quality") String quality) {
		if (file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empty file provided!");
		}

		try {
			STLReader stlReader = new STLReader(file.getInputStream(), unit, materialId, infillPercentage, surfaceThickness);
			double surfaceArea = stlReader.getSurfaceArea();
			//System.out.println("surface area "+surfaceArea);
			double volume = stlReader.getVolume();
			double weight = stlReader.getWeight();
			double[] dimensions=stlReader.getDimensions();

			double filamentPrice = 0.08; //1g of filament in polish zl.
			double powerPrice = weight/8 * 1.15; //Within hour my printer can print something about 8g of filament, and 1kWh in Poland costs around 1.15zl
			double profitMargin = weight*0.5; //0.9zl per gram of filament, it allows to theoretically earn 900zl on one spool of filament, which cost around 80zl.
			double additionalCost=0;

			if(postProcessing.equals("grinding")){
				additionalCost+=(surfaceArea/50)*30; //Within an hour I estimate that I could grind something around 50cm2 and I am calculating my hour work as a 30zl.
			}

			switch(quality){
				case("standard"):
					break;
				case("high"):
					powerPrice*=2;
					additionalCost+=(weight/4)*2; //Additional price because of taken time.
					break;
				case("thick layer"):
					powerPrice/=0.75;
					break;
			}

			if(executionTime.equals("express")){
				additionalCost+=10;
			}

			double endPrice = (weight*filamentPrice+powerPrice+profitMargin+additionalCost)*quantity;


			CalculationResult result = new CalculationResult(surfaceArea, volume, weight, endPrice, dimensions[0], dimensions[1], dimensions[2]);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload the file: " + file.getOriginalFilename() + "!");
		}
	}
	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Backend is running");
	}
}
