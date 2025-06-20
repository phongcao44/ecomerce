package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.ColorRequest;
import com.ra.base_spring_boot.dto.resp.ColorResponse;
import com.ra.base_spring_boot.model.Color;
import com.ra.base_spring_boot.repository.IColorRepository;
import com.ra.base_spring_boot.services.impl.ColorServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/color")
public class ColorController {
    @Autowired
    private ColorServiceImpl colorService;
    @Autowired
    private IColorRepository iColorRepository;
    @Autowired
    private RestTemplate restTemplate;

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


    // https://www.thecolorapi.com (api mã màu)
    // https://www.thecolorapi.com/form-id
    // https://www.thecolorapi.com/id?hex="mã màu"
    @PostMapping("/autoadd")
    public ResponseEntity<?> autoadd(@RequestBody ColorRequest color) {
        if (color == null || color.getHexCode() == null) {
            return ResponseEntity.badRequest().body("Cần cung cấp mã màu (hexCode)");
        }

        // Bỏ dấu #
        String hex = color.getHexCode().replace("#", "").toLowerCase();

        // Chuyển từ 3 ký tự sang 6 ký tự nếu cần
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }

        // Set lại hexCode chuẩn hóa
        color.setHexCode("#" + hex);

        try {
            // Gọi đến TheColorAPI
            String url = "https://www.thecolorapi.com/id?hex=" + hex;
            ColorResponse response = restTemplate.getForObject(url, ColorResponse.class);

//            // Lấy tên màu từ response
//            if (response != null && response.getName() != null) {
//                color.setName(response.getName().getValue());
//            } else {
//                color.setName("Không xác định");
//            }
            Color colornew = new Color();
            colornew.setHexCode("#" + hex);
            colornew.setName((response != null && response.getName() != null)
                    ? response.getName().getValue()
                    : "Không xác định");

            // Lưu DB
            Color saved = colorService.save(colornew);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi gọi API màu: " + e.getMessage());
        }
    }
}
