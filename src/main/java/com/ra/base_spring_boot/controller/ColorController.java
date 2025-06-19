package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.model.Color;
import com.ra.base_spring_boot.repository.IColorRepository;
import com.ra.base_spring_boot.services.impl.ColorServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/color")
public class ColorController {
    @Autowired
    private ColorServiceImpl colorService;
    @Autowired
    private IColorRepository iColorRepository;

    @GetMapping("/list")
    public List<Color> list() {
       return iColorRepository.findAll();
    }
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Color color) {
        if(color == null ||
                color.getName() == null ||
                color.getHexCode() == null) {
            return ResponseEntity.badRequest().body("cần thêm dữ liệu");
        }
        Color savedColor = colorService.save(color);
        return ResponseEntity.ok(savedColor);
    }
    @PutMapping("/edit/{colorId}")
    public ResponseEntity<?> edit(@PathVariable Long colorId,@RequestBody Color newcolor) {
        //kiem tra id
        Optional<Color> colorcheck = iColorRepository.findById(colorId);
        if(colorcheck.isEmpty()) {
            return ResponseEntity.badRequest().body("id khong có");
        }
        //kiem tra body
        if(newcolor == null ||
                newcolor.getName() == null ||
                newcolor.getHexCode() == null) {
            return ResponseEntity.badRequest().body("cần thêm dữ liệu");
        }
        return iColorRepository.findById(colorId).map(color -> {
            color.setName(newcolor.getName());
            color.setHexCode(newcolor.getHexCode());
            Color updatedColor = iColorRepository.save(color);
            return ResponseEntity.ok(updatedColor);
                }).orElseThrow();
    }
    @DeleteMapping("/delete/{colorId}")
    public ResponseEntity<?> delete(@PathVariable Long colorId) {
        Optional<Color> color = iColorRepository.findById(colorId);
        if(color.isEmpty()) {
            return ResponseEntity.badRequest().body("id khong có");
        }
        iColorRepository.deleteById(colorId);
        return ResponseEntity.ok("xoa thanh cong goy");
    }
}
