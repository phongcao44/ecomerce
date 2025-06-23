package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.FlashSaleItemRequest;
import com.ra.base_spring_boot.dto.resp.FlashSaleItemRespone;
import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.repository.IFlashSaleItemRepository;
import com.ra.base_spring_boot.repository.IFlashSaleRepository;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/flash_sale")
public class FlashSaleController {
    @Autowired
    public IFlashSaleRepository flashSaleRepository;
    @Autowired
    public IFlashSaleItemRepository flashSaleItemRepository;
    @Autowired
    public IProductVariantRepository productVariantRepository;

    //flash_sale
    @GetMapping("/list")
    public ResponseEntity<?> getFlashSale() {
        List<FlashSale> flashSales = flashSaleRepository.findAll();
        return new ResponseEntity<>(flashSales, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addFlashSale(@RequestBody FlashSaleResponse request) {
        FlashSale flashSale = FlashSale.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus()).build();
        flashSaleRepository.save(flashSale);
        return new ResponseEntity<>(flashSale, HttpStatus.OK);
    }

    @PostMapping("/edit/{id}")
    public ResponseEntity<?> editFlashSale(@PathVariable Long id, @RequestBody FlashSaleResponse request) {
        Optional<FlashSale> flashSale = flashSaleRepository.findById(id);
        if (flashSale.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        } else {
            FlashSale editFlashSale = flashSale.get();
            editFlashSale.setName(request.getName());
            editFlashSale.setDescription(request.getDescription());
            editFlashSale.setStartTime(request.getStartTime());
            editFlashSale.setEndTime(request.getEndTime());
            editFlashSale.setStatus(request.getStatus());

            FlashSale updatedFlashSale = flashSaleRepository.save(editFlashSale);

            return new ResponseEntity<>(updatedFlashSale, HttpStatus.OK);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFlashSale(@PathVariable Long id) {
        Optional<FlashSale> flashSale = flashSaleRepository.findById(id);
        if (flashSale.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        } else {
            flashSaleRepository.deleteById(id);
            return new ResponseEntity<>("xóa goy", HttpStatus.OK);
        }
    }

    //flash_sale_item
    @GetMapping("/flash_sale_items/{id}")
    public ResponseEntity<?> getFlashSaleItemsByFlashSaleId(@PathVariable Long id) {
        List<FlashSaleItem> items = flashSaleItemRepository.findByFlashSaleId(id);

        if (items.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @PostMapping("/flash_sale_items/add")
    public ResponseEntity<?> addFlashSaleItems(@RequestBody FlashSaleItemRequest request) {
        //laays flash sale
        FlashSale flashSale = flashSaleRepository.findById(request.getFlashSaleId()).
                orElseThrow(() -> new RuntimeException("Không tìm thấy flash sale"));
        //lay variant
        ProductVariant variant = productVariantRepository.findById(request.getVariantId()).
                orElseThrow(() -> new RuntimeException("không thấy productID này"));
        //tạo flashsaleitem mới
        FlashSaleItem flashSaleItem = FlashSaleItem.builder()
                .flashSale(flashSale)
                .variant(variant)
                .quantityLimit(request.getQuantity())
                .soldQuantity(0)
                .discountedPrice(request.getPrice())
                .discountType(request.getDiscountType()).build();
        flashSaleItemRepository.save(flashSaleItem);
        return new ResponseEntity<>("thêm tành công", HttpStatus.OK);
    }
    @PostMapping("/flash_sale_items/edit/{id}")
    public ResponseEntity<?> editFlashSaleItems(@RequestBody FlashSaleItemRespone request, @PathVariable Long id) {
        Optional<FlashSaleItem> flashSaleItem = flashSaleItemRepository.findById(id);
        if (flashSaleItem.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        }else{
            FlashSaleItem editflashSaleItem = flashSaleItem.get();
          //  editflashSaleItem.getFlashSale().setId(id);
            editflashSaleItem.getVariant().setId(request.getVariantId());
            editflashSaleItem.setQuantityLimit(request.getQuantity());
            editflashSaleItem.setSoldQuantity(request.getSoldQuantity());
            editflashSaleItem.setDiscountedPrice(request.getPrice());
            editflashSaleItem.setDiscountType(request.getDiscountType());

            FlashSaleItem updatedFlashSaleItem = flashSaleItemRepository.save(editflashSaleItem);
            return new ResponseEntity<>(updatedFlashSaleItem, HttpStatus.OK);
        }
    }
    @DeleteMapping("/flash_sale_items/delete/{id}")
    public ResponseEntity<?> deleteFlashSaleItems(@PathVariable Long id) {
        Optional<FlashSaleItem> flashSaleItem = flashSaleItemRepository.findById(id);
        if (flashSaleItem.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        }else{
            flashSaleItemRepository.delete(flashSaleItem.get());
            return new ResponseEntity<>("xóa goy", HttpStatus.OK);
        }
    }
}