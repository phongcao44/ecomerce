package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.FlashSaleItemRequest;
import com.ra.base_spring_boot.dto.req.FlashSaleRequest;
import com.ra.base_spring_boot.dto.resp.FlashSaleItemRespone;
import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.repository.IFlashSaleItemRepository;
import com.ra.base_spring_boot.repository.IFlashSaleRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import com.ra.base_spring_boot.services.IFlashSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/flash_sale")
public class FlashSaleController {
    @Autowired
    public IFlashSaleRepository flashSaleRepository;
    @Autowired
    public IFlashSaleItemRepository flashSaleItemRepository;
    @Autowired
    public IProductVariantRepository productVariantRepository;
    @Autowired
    public IProductRepository productRepository;
    @Autowired
    IFlashSaleService flashSaleService;

    //flash_sale
    @GetMapping("/list")
    public ResponseEntity<?> getFlashSaleList() {
        List<FlashSaleResponse> flashSaleResponses = flashSaleService.getFlashSale().toList();
        return ResponseEntity.ok(flashSaleResponses);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addFlashSale(@RequestBody FlashSaleRequest request) {
        FlashSale flashSale = FlashSale.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus())
                .build();
        flashSaleRepository.save(flashSale);
        return new ResponseEntity<>(flashSale, HttpStatus.CREATED);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editFlashSale(@PathVariable Long id, @RequestBody FlashSaleRequest request) {
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Flash Sale"));
        flashSale.setName(request.getName());
        flashSale.setDescription(request.getDescription());
        flashSale.setStartTime(request.getStartTime());
        flashSale.setEndTime(request.getEndTime());
        flashSale.setStatus(request.getStatus());
        flashSaleRepository.save(flashSale);
        return ResponseEntity.ok(flashSale);
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
        List<FlashSaleItemRespone> response = items.stream().map(item -> {
            FlashSaleItemRespone dto = new FlashSaleItemRespone();
            dto.setId(item.getId());

            if (item.getProduct() != null) {
                dto.setProductId(item.getProduct().getId());
                //dto.setProductName(item.getProduct().getName());
            }

            if (item.getVariant() != null) {
                dto.setVariantId(item.getVariant().getId());
               // dto.setVariantName(item.getVariant().getVariantName());
            }

            dto.setDiscountedPrice(item.getDiscountedPrice());
            dto.setQuantityLimit(item.getQuantityLimit());
            dto.setSoldQuantity(item.getSoldQuantity());
            dto.setDiscountType(item.getDiscountType());

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/flash_sale_items/add")
    public ResponseEntity<?> addFlashSaleItems(@RequestBody FlashSaleItemRequest request) {
        //laays flash sale
        FlashSale flashSale = flashSaleRepository.findById(request.getFlashSaleId()).
                orElseThrow(() -> new RuntimeException("Không tìm thấy flash sale"));
        //lay variant
        ProductVariant variant = productVariantRepository.findById(request.getVariantId()).
                orElseThrow(() -> new RuntimeException("không thấy productID này"));
        //lấy product
        Product product = productRepository.findById(request.getProductId()).
                orElseThrow(() -> new RuntimeException("khonng tim thay productid"));
        //tạo flashsaleitem mới
        FlashSaleItem flashSaleItem = FlashSaleItem.builder()
                .flashSale(flashSale)
                .variant(variant)
                .product(product)
                .quantityLimit(request.getQuantity())
                .soldQuantity(0)
                .discountedPrice(request.getPrice())
                .discountType(request.getDiscountType()).build();
        flashSaleItemRepository.save(flashSaleItem);
        return new ResponseEntity<>("thêm tành công", HttpStatus.OK);
    }
    @PostMapping("/flash_sale_items/edit/{id}")
    public ResponseEntity<?> editFlashSaleItems(@RequestBody FlashSaleItemRequest request, @PathVariable Long id) {
        Optional<FlashSaleItem> flashSaleItem = flashSaleItemRepository.findById(id);
        if (flashSaleItem.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        }else{
            FlashSaleItem editflashSaleItem = flashSaleItem.get();
            // Tạo mới Variant và Product với ID từ request(khóa 9 thì tạo mới)
            ProductVariant variant = new ProductVariant();
            variant.setId(request.getVariantId());
            editflashSaleItem.setVariant(variant);

            Product product = new Product();
            product.setId(request.getProductId());
            editflashSaleItem.setProduct(product);
            //(thuộc tính thi gán lại)
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