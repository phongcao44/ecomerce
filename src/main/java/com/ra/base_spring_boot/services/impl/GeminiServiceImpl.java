package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.req.GeminiRequest;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.ChatTurn;
import com.ra.base_spring_boot.model.Product;
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
    private ICartService cartService;

    @Autowired
    private ProductSpecificationService productSpecificationService;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    StringBuilder promptBuilder = new StringBuilder();
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
//        String flashSalePrompt = flashSaleService.getFlashSale().stream()
//                .map(flashSale -> String.format(
//                        "- Tên: %s\n  Mô tả: %s\n  Bắt đầu: %s\n  Kết thúc: %s\n  Trạng thái: %s",
//                        flashSale.getName(),
//                        flashSale.getDescription(),
//                        flashSale.getStartTime(),
//                        flashSale.getEndTime(),
//                        flashSale.getStatus()
//                ))
//                .collect(Collectors.joining("\n"));

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
        //lấy bestselling
        List<Top5Product> top5Products = productService.getTop5LestSellingProducts();
        promptBuilder.append("Dưới đây là 5 sản phẩm bán chạy nhất trong hệ thống:\n");

        for (int i = 0; i < top5Products.size(); i++) {
            Top5Product p = top5Products.get(i);
            promptBuilder.append(String.format(
                    "%d. %s - Giá: %.0f VNĐ - Đã bán: %d lần - Đánh giá trung bình: %.1f⭐ - Lượt xem: %d\n",
                    i + 1,
                    p.getProductName(),
                    p.getPrice(),
                    p.getPurchaseCount(),
                    p.getAverageRating() != null ? p.getAverageRating() : 0,
                    p.getTotalReviews()
            ));
        }
//        List<ProductResponseDTO> top5Products = productService.getTop5BestSellingProducts();
//        promptBuilder.append("Dưới đây là 5 sản phẩm bán chạy nhất trong hệ thống:\n");

//        for (int i = 0; i < top5Products.size(); i++) {
//            ProductResponseDTO p = top5Products.get(i);
//            promptBuilder.append(String.format(
//                    "%d. %s - Giá: %.0f VNĐ - Đã bán: %d lần - Đánh giá trung bình: %.1f⭐ - Lượt đánh giá: %d\n",
//                    i + 1,
//                    p.getName(),
//                    p.getDiscountedPrice() != null ? p.getDiscountedPrice().doubleValue() : p.getPrice().doubleValue(),
//                    p.getVariants().stream()
//                            .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
//                            .sum(),
//                    p.getAverageRating() != null ? p.getAverageRating() : 0.0,
//                    p.getTotalReviews() != null ? p.getTotalReviews() : 0
//            ));
//        }

        promptBuilder.append("\nBạn muốn xem thêm thông tin về sản phẩm nào không?\n\n");
        //lấy đồ ế lòi ra
        List<Top5Product> leastSelling = productService.getTop5LestSellingProducts();

        promptBuilder.append("\nNgoài ra, dưới đây là 5 sản phẩm bán chậm nhất:\n");

        for (int i = 0; i < leastSelling.size(); i++) {
            Top5Product p = leastSelling.get(i);

            promptBuilder.append(String.format(
                    "%d. %s - Giá: %.0f VNĐ - Đã bán: %d lần - Đánh giá trung bình: %.1f⭐ - Lượt xem: %d\n",
                    i + 1,
                    p.getProductName(),
                    p.getPrice(),
                    p.getPurchaseCount(),
                    p.getAverageRating() != null ? p.getAverageRating() : 0,
                    p.getTotalReviews()
            ));
        }

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
        String effectivePrompt = request.getPrompt();
        if (isGenericConfirmation(normalize(effectivePrompt)) && lastUserMentionedProduct != null) {
            effectivePrompt = lastUserMentionedProduct + " " + effectivePrompt;
        }

        // 10. Prompt gửi lên Gemini
        String fullPrompt = String.format("""
                Bạn là một trợ lý AI tư vấn bán hàng xưng em.
                Nếu có liệu kê luôn xuống dòng để đẹp mắt
                Nếu từ khóa khách hàng có liên quan đến sản phẩm bạn có thể tương tác nhưng mục đích sau cuối vẫn là bán hàng.
                Bạn được phép tư vấn nếu khách hàng yêu cầu(ví dụ tư vấn màu thích hợp màu da, hoặc ca sản phẩm có thể phù hợp với nhu cầu khách hàng).
<<<<<<< src/main/java/com/ra/base_spring_boot/services/impl/GeminiServiceImpl.java
                Nếu bạn đang được hỏi về các sản phẩm bán chậm, bán ế,.. hãy kèm theo voucher "GEMINIUUDAI" để thu hút hơn
=======
                Nếu bạn đang được hỏi về các sản phẩm bán chậm, bán ế,.. lun liệt kê top 5 và hãy kèm theo voucher "GEMINIUUDAI" để thu hút hơn
>>>>>>> src/main/java/com/ra/base_spring_boot/services/impl/GeminiServiceImpl.java
                Bạn cũng được phép tư vấn các sản phẩm liên quan tới nhu cầu như quần áo để mặc, điện thoại để nghe gọi và cố gắng thuyết phục khách hàng mua hàng
                Nếu khách hàng yêu cầu cung cấp thông tin về sản phẩm thì bạn phải cung cấp thông tin chứ không hoàn toàn tập trung vào việc tư vấn.
                Nếu người dùng trả lời ngắn gọn như "có", "ok", "muốn biết thêm", "tiếp tục", v.v... hãy tiếp tục dựa trên sản phẩm đã được đề cập gần nhất trong lịch sử hội thoại. Đừng tự suy diễn sai sang sản phẩm khác.
                Nếu sản phẩm không có biến thể hay không có các thông tin như sách hàng muốn cung cấp hãy trả lời thẳn thắng rằng bạn chưa tìm thấy thông tin đó
                Nếu người dùng xác nhận hoặc yêu cầu tiếp tục nhưng không nói rõ sản phẩm mới, bạn hãy tiếp tục tư vấn dựa trên sản phẩm đã nhắc đến gần nhất:
                Bạn có quyền cung cấp thông tin từ sản phẩm trước đó ví dụ:
                Nếu khách trả lời không rõ ràng hoặc xác nhận mơ hồ (ví dụ: "có", "ok", "tiếp tục", "xem thêm",...), bạn hãy chủ động:
                - Nếu trước đó có đề cập đến một nhóm sản phẩm (ví dụ: áo MU 2023–2025), hãy chọn **ngẫu nhiên một sản phẩm trong nhóm** để tiếp tục tư vấn.
                - Nếu chỉ có một sản phẩm cụ thể được nói đến gần nhất, hãy tiếp tục tư vấn sâu hơn về sản phẩm đó.
                - Nếu khách đề cập đến mục đích sử dụng (ví dụ: "lập trình"), hãy chọn sản phẩm phù hợp (ví dụ: máy tính cấu hình cao).
                Khi khách hàng yêu cầu cung cấp dữ liệu chỉ sử dụng dữ liệu được cung cấp dưới đây. Không tự bịa thêm thông tin nếu dữ liệu không đề cập.
                Nếu người dùng hỏi ngoài phạm vi, hãy trả lời: "Xin lỗi, tôi chỉ hỗ trợ tư vấn sản phẩm tại shop này."


                Danh sách sản phẩm:
                %s

                Danh sách biến thể sản phẩm:
                %s

                Danh sách màu sắc:
                %s

                Danh sách hình ảnh sản phẩm:
                %s

                Danh sách sản phẩm bán chạy:
                %s

                Danh sách sản phẩm có lượt bán thấp:
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
                top5Products,
                leastSelling,
//                flashSalePrompt,
                flashSalePrompt,
                focusProductContext,
                historyContext,
                effectivePrompt);

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
                    String geminiReply = (String) ((Map) parts.get(0)).get("text");
                    //them gio

                    Optional<CartItemRequestDTO> cartItemOpt = extractCartItemFromPrompt(effectivePrompt);

                    if (cartItemOpt.isPresent() && request.getUserId() != null) {
                        CartItemRequestDTO dto = cartItemOpt.get();
                        cartService.addItemToCart(request.getUserId(), dto);

                        // Tìm lại thông tin biến thể để xác nhận
                        ProductVariantDetailDTO variant = variants.stream()
                                .filter(v -> v.getVariantId().equals(dto.getVariantId()))
                                .findFirst()
                                .orElse(null);

                        if (variant != null) {
                            String confirmMsg = String.format("Đã thêm **%s %s %s** vào giỏ hàng của bạn. Giá: %,.0fđ. " +
                                            "Hãy truy cập giỏ hàng của bạn và thanh toán, ngoài ra bạn có thể truy cập hồ sơ và nhận được nhiều voucher hấp dẫn đấy đấy ạ",
                                    variant.getProductName(),
                                    variant.getColorName() != null ? "(Màu: " + variant.getColorName() + ")" : "",
                                    variant.getSizeName() != null ? "(Size: " + variant.getSizeName() + ")" : "",
                                    variant.getPriceOverride() != null ? variant.getPriceOverride() : variant.getPrice()
                            );
                            return new GeminiResponse(confirmMsg);
                        }
                        System.out.println(">>> Đã thêm biến thể vào giỏ: " + dto.getVariantId());
                        System.out.println(">>> User ID: " + request.getUserId());
                        return new GeminiResponse("Sản phẩm đã được thêm vào giỏ hàng của bạn!");
                    }
                    if (request.getUserId() == null) {
                        System.out.println(">>> userId NULL => KHÔNG thêm vào giỏ");
                    }
                    if (cartItemOpt.isEmpty()) {
                        System.out.println(">>> Prompt KHÔNG chứa sản phẩm cụ thể => KHÔNG thêm vào giỏ");
                    }


                    return new GeminiResponse(geminiReply);
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
    private Optional<CartItemRequestDTO> extractCartItemFromPrompt(String prompt) {
        System.out.println(">>> [extractCartItemFromPrompt] Prompt nhận vào: " + prompt);

        String normalized = normalize(prompt);
        List<String> shoppingIntents = List.of("them vao gio", "mua", "dat mua", "mua ngay", "chon mua", "them gio", "them 1 cai", "them hang");

        boolean matchIntent = (
                (normalized.contains("them") && normalized.contains("gio")) ||
                        normalized.contains("mua")
        );

        if (!matchIntent) {
            System.out.println(">>> Prompt KHÔNG chứa ý định mua hàng");
            return Optional.empty();
        }


        List<ProductResponseDTO> products = productService.findAll();
        List<ProductVariantDetailDTO> variants = productVariantService.findAllVariantDetails();

        System.out.println(">>> Có tất cả " + products.size() + " sản phẩm và " + variants.size() + " biến thể");

        Optional<Product> matchedProductOpt = findBestMatchedProduct(
                products.stream().map(p -> {
                    Product prod = new Product();
                    prod.setName(p.getName());
                    return prod;
                }).collect(Collectors.toList()),
                normalized
        );


        if (matchedProductOpt.isEmpty()) {
            System.out.println(">>> KHÔNG tìm thấy sản phẩm phù hợp với prompt");
            return Optional.empty();
        }
        ProductResponseDTO matchedProduct = products.stream()
                .filter(p -> p.getName().equals(matchedProductOpt.get().getName()))
                .findFirst().orElse(null);


        for (ProductVariantDetailDTO variant : variants) {
            if (!variant.getProductName().equals(matchedProduct.getName())) continue;

            System.out.println(">>> Kiểm tra biến thể: variantId = " + variant.getVariantId()
                    + ", color = " + variant.getColorName()
                    + ", size = " + variant.getSizeName());

            boolean hasColor = variant.getColorName() != null;
            boolean hasSize = variant.getSizeName() != null;

            boolean matchColor = !hasColor || normalized.contains(normalize(variant.getColorName()));
            boolean matchSize = !hasSize || normalized.contains(normalize(variant.getSizeName()));

            System.out.println(">>> So khớp biến thể:");
            System.out.println("    - matchColor(" + variant.getColorName() + "): " + matchColor);
            System.out.println("    - matchSize(" + variant.getSizeName() + "): " + matchSize);

            if (matchColor && matchSize) {
                System.out.println(">>> Match biến thể phù hợp: " + variant.getVariantId() + " - " + variant.getColorName() + " - " + variant.getSizeName());
                CartItemRequestDTO dto = new CartItemRequestDTO();
                dto.setVariantId(variant.getVariantId());
                dto.setQuantity(1);
                return Optional.of(dto);
            }
        }

        System.out.println(">>> Có sản phẩm nhưng KHÔNG tìm được biến thể phù hợp");
        return Optional.empty();
    }

    // chặt thành khúc
    private Optional<Product> findBestMatchedProduct(List<Product> products, String normalizedPrompt) {
        String[] promptTokens = normalizedPrompt.split(" ");

        Product bestProduct = null;
        int bestMatchScore = 0;

        for (Product p : products) {
            String productName = normalize(p.getName());
            String[] productTokens = productName.split(" ");

            // Đếm số từ trong tên sản phẩm xuất hiện trong prompt
            int matchCount = 0;
            for (String token : productTokens) {
                if (Arrays.asList(promptTokens).contains(token)) {
                    matchCount++;
                }
            }

            if (matchCount > bestMatchScore) {
                bestMatchScore = matchCount;
                bestProduct = p;
            }
        }

        // Match được ít nhất 2 từ thì mới tin là đúng
        return (bestMatchScore >= 2) ? Optional.of(bestProduct) : Optional.empty();
    }


}
