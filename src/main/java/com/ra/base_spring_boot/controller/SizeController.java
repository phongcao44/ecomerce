package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.SizeRequest;
import com.ra.base_spring_boot.model.Size;
import com.ra.base_spring_boot.repository.ICategoryRepository;
import com.ra.base_spring_boot.repository.ISizeRepository;
import com.ra.base_spring_boot.services.ISizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class SizeController {
    @Autowired
    public ISizeService iSizeService;
    @Autowired
    public ISizeRepository iSizeRepository;
    @GetMapping("/list")
    public List<Size> list() {
        return iSizeService.findAll();
    }

    @PostMapping("/admin/size/add")
    public ResponseEntity<?> save(@RequestBody SizeRequest size) {
        List<Size> optional = iSizeService.findAll();
        //bat loi nhap du lieu rong ""
        for(Size s: optional){
            if (s.getDescription().isEmpty()) {
                return ResponseEntity.badRequest().body("banj can bo sung day du du lieu");
            }
        }
        if(size == null ||
                size.getName() == null ||
                size.getDescription() == null) {
           return ResponseEntity.badRequest().body("can nhap ten name va description");
        }
        Size sizeEntity = new Size();
        sizeEntity.setSizeName(size.getName());
        sizeEntity.setDescription(size.getDescription());
        Size saved = iSizeService.save(sizeEntity);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/admin/size/edit/{sizeId}")
    public ResponseEntity<?> edit(@PathVariable Long sizeId, @RequestBody SizeRequest newsize) {
        Optional<Size> idcheck = iSizeRepository.findById(sizeId);
        if(idcheck.isEmpty()) {
            return ResponseEntity.badRequest().body("id khong có");
        }
        if(newsize == null ||
        newsize.getName() == null ||
                newsize.getDescription() == null) {
            return ResponseEntity.badRequest().body("can nhap ten name va description");
        }
        return iSizeRepository.findById(sizeId).map(size ->{
            size.setSizeName(newsize.getName());
            size.setDescription(newsize.getDescription());
            Size saved = iSizeRepository.save(size);
            return ResponseEntity.ok(saved);
                }).orElseThrow();
    }
    @DeleteMapping("/admin/size/delete/{sizeId}")
    public ResponseEntity<?> delete(@PathVariable Long sizeId) {
        Optional<Size> idcheck = iSizeRepository.findById(sizeId);
        if(idcheck.isEmpty()) {
            return ResponseEntity.badRequest().body("id khong có");
        }
        iSizeRepository.deleteById(sizeId);
        return ResponseEntity.ok("xoa thanh cong goy");
    }
}
