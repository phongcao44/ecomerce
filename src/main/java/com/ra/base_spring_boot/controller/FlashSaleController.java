package com.ra.base_spring_boot.controller;

import org.springframework.data.domain.Page;
import com.ra.base_spring_boot.dto.req.FlashSaleItemRequest;
import com.ra.base_spring_boot.dto.req.FlashSaleRequest;
import com.ra.base_spring_boot.dto.resp.FlashSaleItemRespone;
import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.dto.resp.FlashSaleVariantDetailResponse;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.repository.IFlashSaleItemRepository;
import com.ra.base_spring_boot.repository.IFlashSaleRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import com.ra.base_spring_boot.services.IFlashSaleItemService;
import com.ra.base_spring_boot.services.IFlashSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public IFlashSaleService flashSaleService;
    @Autowired
    public IFlashSaleItemService flashSaleItemService;
    @GetMapping("/flash_sale_items/detail/{id}")
    public ResponseEntity<?> getFlashSaleVariantDetailsByFlashSaleId(@PathVariable Long id) {
        try {
            List<FlashSaleVariantDetailResponse> responses = flashSaleService.getFlashSaleItemsByFlashSaleId(id);
            return ResponseEntity.ok(responses);
        } catch (ChangeSetPersister.NotFoundException e) {
            return new ResponseEntity<>("Không tìm thấy flash sale với ID: " + id, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveFlashSale() {
        LocalDateTime now = LocalDateTime.now();
        Optional<FlashSale> activeFlashSale = flashSaleRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now);

        if (activeFlashSale.isPresent()) {
            return new ResponseEntity<>(activeFlashSale.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Không tìm thấy flash sale đang diễn ra", HttpStatus.NOT_FOUND);
        }
    }

    //flash_sale
    @GetMapping("/list")
    public ResponseEntity<?> getFlashSale() {
        List<FlashSale> flashSales = flashSaleRepository.findAll();
        return new ResponseEntity<>(flashSales, HttpStatus.OK);
    }

    @GetMapping("/{flashSaleId}/items")
    public Page<ProductResponseDTO> getFlashSaleItems(
            @PathVariable Long flashSaleId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String discountRange,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String orderBy) {
        return flashSaleService.getFlashSaleItemsPaginate(
                flashSaleId, categoryId, brand, minPrice, maxPrice, discountRange, minRating, page, limit, sortBy, orderBy);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getFlashSaleDetails(@PathVariable("id") Long flashSaleId) {
        List<ProductResponseDTO> responses = flashSaleService.getFlashSaleDetails(flashSaleId);
        if (responses.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(responses);
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
        try {
            // Lấy flash sale
            FlashSale flashSale = flashSaleRepository.findById(request.getFlashSaleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy flash sale"));

            // Lấy variant
            ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Không thấy productVariant này"));

            // Lấy product
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy product này"));

            // Validation: Kiểm tra xem variant đã tồn tại trong flash sale này chưa
            boolean variantExists = flashSaleItemRepository.existsByFlashSaleIdAndVariantId(
                    request.getFlashSaleId(), request.getVariantId());

            if (variantExists) {
                return new ResponseEntity<>("Biến thể sản phẩm này đã tồn tại trong flash sale",
                        HttpStatus.CONFLICT);
            }

            // Validation: Kiểm tra variant có thuộc về product không
            if (!variant.getProduct().getId().equals(request.getProductId())) {
                return new ResponseEntity<>("Biến thể sản phẩm không thuộc về sản phẩm này",
                        HttpStatus.BAD_REQUEST);
            }

            // Validation: Kiểm tra flash sale còn active không
            if (flashSale.getEndTime().isBefore(LocalDateTime.now())) {
                return new ResponseEntity<>("Flash sale đã kết thúc, không thể thêm sản phẩm mới",
                        HttpStatus.BAD_REQUEST);
            }

            // Validation: Kiểm tra số lượng hợp lệ
            if (request.getQuantity() <= 0) {
                return new ResponseEntity<>("Số lượng phải lớn hơn 0", HttpStatus.BAD_REQUEST);
            }

            // Validation: Kiểm tra giá giảm hợp lệ
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseEntity<>("Giá sản phẩm phải lớn hơn 0", HttpStatus.BAD_REQUEST);
            }

            // Tạo flashsaleitem mới
            FlashSaleItem flashSaleItem = FlashSaleItem.builder()
                    .flashSale(flashSale)
                    .variant(variant)
                    .product(product)
                    .quantityLimit(request.getQuantity())
                    .soldQuantity(0)
                    .discountedPrice(request.getPrice())
                    .discountType(request.getDiscountType())
                    .build();

            flashSaleItemRepository.save(flashSaleItem);
            return new ResponseEntity<>("Thêm thành công", HttpStatus.OK);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/flash_sale_items/edit/{id}")
    public ResponseEntity<?> editFlashSaleItems(@RequestBody FlashSaleItemRequest request,
                                                @PathVariable Long id) {
        try {
            Optional<FlashSaleItem> flashSaleItemOpt = flashSaleItemRepository.findById(id);

            if (flashSaleItemOpt.isEmpty()) {
                return new ResponseEntity<>("Không tìm thấy sản phẩm nào trong flash sale này",
                        HttpStatus.NOT_FOUND);
            }

            FlashSaleItem existingFlashSaleItem = flashSaleItemOpt.get();

            // Lấy variant và product mới
            ProductVariant newVariant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy productVariant này"));

            Product newProduct = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy product này"));

            // Validation: Kiểm tra variant có thuộc về product không
            if (!newVariant.getProduct().getId().equals(request.getProductId())) {
                return new ResponseEntity<>("Biến thể sản phẩm không thuộc về sản phẩm này",
                        HttpStatus.BAD_REQUEST);
            }

            // Validation: Nếu thay đổi variant, kiểm tra variant mới có bị trùng không
            if (!existingFlashSaleItem.getVariant().getId().equals(request.getVariantId())) {
                boolean variantExists = flashSaleItemRepository.existsByFlashSaleIdAndVariantIdAndIdNot(
                        existingFlashSaleItem.getFlashSale().getId(),
                        request.getVariantId(),
                        id);

                if (variantExists) {
                    return new ResponseEntity<>("Biến thể sản phẩm này đã tồn tại trong flash sale",
                            HttpStatus.CONFLICT);
                }
            }

            // Validation: Kiểm tra flash sale còn active không
            if (existingFlashSaleItem.getFlashSale().getEndTime().isBefore(LocalDateTime.now())) {
                return new ResponseEntity<>("Flash sale đã kết thúc, không thể chỉnh sửa",
                        HttpStatus.BAD_REQUEST);
            }

            // Validation: Kiểm tra số lượng hợp lệ
            if (request.getQuantity() <= 0) {
                return new ResponseEntity<>("Số lượng phải lớn hơn 0", HttpStatus.BAD_REQUEST);
            }

            // Validation: Số lượng mới không được nhỏ hơn số lượng đã bán
            if (request.getQuantity() < existingFlashSaleItem.getSoldQuantity()) {
                return new ResponseEntity<>(
                        String.format("Số lượng giới hạn (%d) không được nhỏ hơn số lượng đã bán (%d)",
                                request.getQuantity(), existingFlashSaleItem.getSoldQuantity()),
                        HttpStatus.BAD_REQUEST);
            }

            // Validation: Kiểm tra sold quantity hợp lệ
            if (request.getSoldQuantity() < 0 || request.getSoldQuantity() > request.getQuantity()) {
                return new ResponseEntity<>("Số lượng đã bán không hợp lệ", HttpStatus.BAD_REQUEST);
            }

            // Validation: Kiểm tra giá giảm hợp lệ
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseEntity<>("Giá sản phẩm phải lớn hơn 0", HttpStatus.BAD_REQUEST);
            }

            // Cập nhật thông tin
            existingFlashSaleItem.setVariant(newVariant);
            existingFlashSaleItem.setProduct(newProduct);
            existingFlashSaleItem.setQuantityLimit(request.getQuantity());
            existingFlashSaleItem.setSoldQuantity(request.getSoldQuantity());
            existingFlashSaleItem.setDiscountedPrice(request.getPrice());
            existingFlashSaleItem.setDiscountType(request.getDiscountType());

            FlashSaleItem updatedFlashSaleItem = flashSaleItemRepository.save(existingFlashSaleItem);
            return new ResponseEntity<>(updatedFlashSaleItem, HttpStatus.OK);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
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
    @GetMapping("/flash_sale_items/top1")
    public ResponseEntity<?> getTop1(
    ){
      FlashSaleResponse flashSaleResponse =  flashSaleItemService.getTop1();
      return new ResponseEntity<>(flashSaleResponse, HttpStatus.OK);
    }
}