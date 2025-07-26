package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.GeminiRequest;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.ChatTurn;
import com.ra.base_spring_boot.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private IColorService colorService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IProductVariantService productVariantService;

    @Autowired
    private ProductImageServiceImpl productImageService;

    @Autowired
    private ISizeService iSizeService;

    @Autowired
    private IFlashSaleService flashSaleService;

    @Autowired
    private ProductSpecificationService productSpecificationService;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    @Override
    public GeminiResponse generateContent(GeminiRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Lấy dữ liệu màu sắc
        String colorPrompt = colorService.findAllColors().stream()
                .map(color -> String.format("- Tên màu: %s (Mã: %s)", color.getName(), color.getHex_code()))
                .collect(Collectors.joining("\n"));

        // 2. Lấy kích thước
        String SizePrompt = iSizeService.findAll().stream()
                .map(size -> String.format("tên: %s (Mô: %s )", size.getSizeName(), size.getDescription()))
                .collect(Collectors.joining("\n"));

        // 3. Lấy chương trình flash sale
        String flashSalePrompt = flashSaleService.getFlashSale().stream()
                .map(flashSale -> String.format(
                        "- Tên: %s\n  Mô tả: %s\n  Bắt đầu: %s\n  Kết thúc: %s\n  Trạng thái: %s",
                        flashSale.getName(),
                        flashSale.getDescription(),
                        flashSale.getStartTime(),
                        flashSale.getEndTime(),
                        flashSale.getStatus()
                ))
                .collect(Collectors.joining("\n"));

        // 4. Lấy thông tin sản phẩm
        List<ProductResponseDTO> products = productService.findAll();
        String productInfo = products.stream()
                .map(product -> "- " + product.getName()
                        + (product.getBrand() != null ? " | Thương hiệu: " + product.getBrand() : "")
                        + (product.getPrice() != null ? " | Giá: " + product.getPrice() + "đ" : "")
                        + (product.getCategoryName() != null ? " | Danh mục: " + product.getCategoryName() : ""))
                .collect(Collectors.joining("\n"));

        // 5. Lấy biến thể sản phẩm
        List<ProductVariantDetailDTO> variants = productVariantService.findAllVariantDetails();
        String variantPrompt = variants.stream()
                .map(v -> String.format("- Sản phẩm: %s | Màu: %s | Size: %s | Giá: %s%s | Còn: %d cái",
                        v.getProductName(),
                        v.getColorName() != null ? v.getColorName() : "N/A",
                        v.getSizeName() != null ? v.getSizeName() : "N/A",
                        v.getPriceOverride() != null ? v.getPriceOverride() + "đ (giá khuyến mãi)" : v.getPrice() + "đ",
                        v.getPriceOverride() != null ? " (giá gốc: " + v.getPrice() + "đ)" : "",
                        v.getStockQuantity()))
                .collect(Collectors.joining("\n"));

        // 6. Lấy thông số kỹ thuật
        List<ProductSpecificationResponseDTO> specs = productSpecificationService.getAll();
        Map<String, List<ProductSpecificationResponseDTO>> specsByProduct = specs.stream()
                .collect(Collectors.groupingBy(ProductSpecificationResponseDTO::getProductName));
        StringBuilder specPrompt = new StringBuilder("Thông số kỹ thuật sản phẩm:\n");
        specsByProduct.forEach((productName, productSpecs) -> {
            specPrompt.append("- ").append(productName).append(":\n");
            for (ProductSpecificationResponseDTO spec : productSpecs) {
                specPrompt.append("    + ").append(spec.getSpecKey())
                        .append(": ").append(spec.getSpecValue()).append("\n");
            }
        });

        // 7. Lấy ảnh sản phẩm theo sản phẩm được nhắc gần nhất
        String lastUserMentionedProduct = extractLastMentionedProduct(request.getHistory(), request.getPrompt());

        String focusProductKey = lastUserMentionedProduct != null ? normalize(lastUserMentionedProduct) : null;
        List<ProductImageResponseDTO> images = productImageService.findAll();
        Map<String, List<ProductImageResponseDTO>> imageMap = images.stream()
                .collect(Collectors.groupingBy(img -> normalize(img.getProductName())));
        System.out.println(">>> focusProductKey: " + focusProductKey);
        System.out.println(">>> Available image keys:");
        imageMap.keySet().forEach(System.out::println);

        String imagePrompt;
        if (focusProductKey != null && (!imageMap.containsKey(focusProductKey) || imageMap.get(focusProductKey).isEmpty())) {
            imagePrompt = "Hiện tại chưa có hình ảnh cho sản phẩm \"" + lastUserMentionedProduct + "\".";
        } else if (focusProductKey != null) {
            imagePrompt = imageMap.get(focusProductKey).stream()
                    .map(img -> String.format("* **%s**%s:\n![Hình ảnh](%s)",
                            img.getProductName(),
                            (img.getVariantColor() != null || img.getVariantSize() != null)
                                    ? " (Màu: " + img.getVariantColor() + ", Size: " + img.getVariantSize() + ")"
                                    : "",
                            img.getImageUrl()))
                    .collect(Collectors.joining("\n"));
        } else {
            imagePrompt = "Không xác định được sản phẩm để hiển thị hình ảnh.";
        }

        // 8. Lịch sử hội thoại và gợi ý sản phẩm liên quan
        String historyContext = request.getHistory() != null ? request.getHistory().stream()
                .map(turn -> (turn.getRole().equalsIgnoreCase("user") ? "Người dùng" : "Trợ lý") + ": " + turn.getMessage())
                .collect(Collectors.joining("\n")) : "";
        if (!historyContext.isEmpty()) {
            historyContext = "Lịch sử hội thoại trước đó:\n" + historyContext;
        }

        String focusProductContext = lastUserMentionedProduct != null
                ? "Sản phẩm đang được nói đến: " + lastUserMentionedProduct + "\n\n" : "";

        // 9. Điều chỉnh prompt nếu xác nhận mơ hồ
        String normalizedPrompt = normalize(request.getPrompt());
        if (isGenericConfirmation(normalizedPrompt) && lastUserMentionedProduct != null) {
            request.setPrompt("Tôi muốn biết thêm về " + lastUserMentionedProduct);
        }

        // 10. Prompt gửi lên Gemini
        String fullPrompt = String.format("""
                Bạn là một trợ lý AI tư vấn bán hàng.
                [...] // Rút gọn phần mô tả vai trò

                Danh sách sản phẩm:
                %s

                Danh sách biến thể sản phẩm:
                %s

                Danh sách màu sắc:
                %s

                Danh sách hình ảnh sản phẩm:
                %s

                Danh sách kích thước:
                %s

                Thông số kỹ thuật:
                %s

                %s
                %s
                %s

                Người dùng hiện tại hỏi: \"%s\"
                """,
                productInfo,
                variantPrompt,
                colorPrompt,
                imagePrompt,
                SizePrompt,
                specPrompt,
                flashSalePrompt,
                focusProductContext,
                historyContext,
                request.getPrompt());

        // 11. Gửi yêu cầu đến Gemini
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", fullPrompt);
        contents.add(Map.of("parts", List.of(contentPart)));
        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + "?key=" + apiKey,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            List candidates = (List) response.getBody().get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map firstCandidate = (Map) candidates.get(0);
                Map contentMap = (Map) firstCandidate.get("content");
                List parts = (List) contentMap.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return new GeminiResponse((String) ((Map) parts.get(0)).get("text"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new GeminiResponse("Không nhận được phản hồi từ Gemini API.");
    }

    private String extractLastMentionedProduct(List<ChatTurn> history, String fallbackPrompt) {
        List<ProductResponseDTO> products = productService.findAll();
        Map<String, String> normalizedProductNames = products.stream()
                .collect(Collectors.toMap(
                        p -> normalize(p.getName()),
                        ProductResponseDTO::getName
                ));

        String lastFound = null;

        // 1. Ưu tiên kiểm tra trong lịch sử hội thoại
        if (history != null && !history.isEmpty()) {
            for (ChatTurn turn : history) {
                String message = turn.getMessage();
                if (message == null) continue;

                String normalizedMessage = normalize(message);
                if ("user".equalsIgnoreCase(turn.getRole()) && isGenericConfirmation(normalizedMessage)) continue;

                for (Map.Entry<String, String> entry : normalizedProductNames.entrySet()) {
                    if (normalizedMessage.contains(entry.getKey())) return entry.getValue();
                }
                for (Map.Entry<String, String> entry : normalizedProductNames.entrySet()) {
                    for (String token : entry.getKey().split(" ")) {
                        if (normalizedMessage.contains(token)) lastFound = entry.getValue();
                    }
                }
            }
            if (lastFound != null) return lastFound;
        }

        // 2. Nếu không tìm thấy trong history, kiểm tra fallbackPrompt
        if (fallbackPrompt != null && !fallbackPrompt.isBlank()) {
            String normalizedPrompt = normalize(fallbackPrompt);
            for (Map.Entry<String, String> entry : normalizedProductNames.entrySet()) {
                if (normalizedPrompt.contains(entry.getKey())) return entry.getValue();
            }
            for (Map.Entry<String, String> entry : normalizedProductNames.entrySet()) {
                for (String token : entry.getKey().split(" ")) {
                    if (normalizedPrompt.contains(token)) return entry.getValue();
                }
            }
        }

        return null;
    }


    private boolean isGenericConfirmation(String message) {
        return List.of("co", "ok", "tiep tuc", "muon biet them", "xem them",
                "dung", "chuan", "phai", "dung roi").contains(message);
    }

    private String normalize(String input) {
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .toLowerCase()
                .trim();
    }
}
