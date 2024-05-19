package com.fsz._3Dcostestimator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/materials")
public class MaterialController {
    private final Materials materials;
    public MaterialController(){
        materials = new Materials();
    }

    @GetMapping("/list")
    public ResponseEntity<Map<Integer, Map<String, Double>>> getMaterials(){
        return ResponseEntity.ok(materials.getListOfMaterials());
    }
}
