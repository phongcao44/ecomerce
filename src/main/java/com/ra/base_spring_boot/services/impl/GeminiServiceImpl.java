package com.ra.base_spring_boot.services.impl;





import com.ra.base_spring_boot.dto.req.GeminiRequest;

import com.ra.base_spring_boot.dto.resp.GeminiResponse;

import com.ra.base_spring_boot.dto.resp.ProductImageResponseDTO;

import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;

import com.ra.base_spring_boot.dto.resp.ProductVariantDetailDTO;

import com.ra.base_spring_boot.model.ChatTurn;

import com.ra.base_spring_boot.repository.IColorRepository;

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
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";



    @Override

    public GeminiResponse generateContent(GeminiRequest request) {

        RestTemplate restTemplate = new RestTemplate();
        // Lấy dữ liệu màu
        String colorPrompt = colorService.findAllColors().stream()
                .map(color -> String.format("- Tên màu: %s (Mã: %s)", color.getName(), color.getHex_code()))
                .collect(Collectors.joining("\n"));
        //lấy kích thước
        String SizePrompt = iSizeService.findAll().stream()
                .map(size ->String.format("tên: %s (Mô: %s )",size.getSizeName(),size.getDescription()))
                .collect(Collectors.joining("\n"));
        //ly trương trình km
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
        // Lấy dữ liệu sản phẩm
        List<ProductResponseDTO> products = productService.findAll();

        String productInfo = products.stream()

                .map(product -> "- " + product.getName()
                        + (product.getBrand() != null ? " | Thương hiệu: " + product.getBrand() : "")
                        + (product.getPrice() != null ? " | Giá: " + product.getPrice() + "đ" : "")
                        + (product.getCategoryName() != null ? " | Danh mục: " + product.getCategoryName() : ""))
                .collect(Collectors.joining("\n"));

                // Lấy dữ liệu biến thể sản phẩm
        List<ProductVariantDetailDTO> variants = productVariantService.findAllVariantDetails();

        String variantPrompt = variants.stream()

                .map(v -> "- Sản phẩm: %s | Màu: %s | Size: %s | Giá: %s%s | Còn: %d cái"

                        .formatted(
                                v.getProductName(),
                                v.getColorName() != null ? v.getColorName() : "N/A",
                                v.getSizeName() != null ? v.getSizeName() : "N/A",
                                v.getPriceOverride() != null ? v.getPriceOverride() + "đ (giá khuyến mãi)" : v.getPrice() + "đ",
                                v.getPriceOverride() != null ? " (giá gốc: " + v.getPrice() + "đ)" : "",
                                v.getStockQuantity()

                        ))

                .collect(Collectors.joining("\n"));

                //lấy dữ liệu hình ảnh
        List<ProductImageResponseDTO> images = productImageService.findAll();
        String imagePrompt = images.stream()
                .map(img -> """

                * **%s**%s:
                ![Hình ảnh](%s)
                """.formatted(
                                img.getProductName(),
                                (img.getVariantColor() != null || img.getVariantSize() != null)
                                        ? " (Màu: " + img.getVariantColor() + ", Size: " + img.getVariantSize() + ")"
                                        : "",
                                img.getImageUrl()
                        )
                )
                .collect(Collectors.joining("\n"));
        String historyContext = "";
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {

            historyContext = request.getHistory().stream()
                    .map(turn -> (turn.getRole().equalsIgnoreCase("user") ? "Người dùng" : "Trợ lý") + ": " + turn.getMessage())
                    .collect(Collectors.joining("\n"));
            historyContext = "Lịch sử hội thoại trước đó:\n" + historyContext;

        }
    //thuốc bổ não
        String lastUserMentionedProduct = extractLastMentionedProduct(request.getHistory());
        String focusProductContext = "";
        if (lastUserMentionedProduct != null) {

            focusProductContext = "Sản phẩm đang được nói đến: " + lastUserMentionedProduct + "\n\n";

        }
        String normalizedPrompt = normalize(request.getPrompt());
        if (isGenericConfirmation(normalizedPrompt) && lastUserMentionedProduct != null) {
                // Gắn lại prompt rõ ràng để Gemini hiểu rõ ngữ cảnh
            request.setPrompt("Tôi muốn biết thêm về " + lastUserMentionedProduct);
        }
                // Ghép prompt đầy đủ
        String fullPrompt = """
                Bạn là một trợ lý AI tư vấn bán hàng.
               Nếu từ khóa khách hàng có liên quan đến sản phẩm bạn có thể tương tác nhưng mục đích sau cuối vẫn là bán hàng.
               Bạn được phép tư vấn nếu khách hàng yêu cầu(ví dụ tư vấn màu thích hợp màu da, hoặc ca sản phẩm có thể phù hợp với nhu cầu khách hàng).
               Bạn cũng được phép tư vấn các sản phẩm liên quan tới nhu cầu như quần áo để mặc, điện thoại để nghe gọi và cố gắng thuyết phục khách hàng mua hàng
               Nếu khách hàng yêu cầu cung cấp thông tin về sản phẩm thì bạn phải cung cấp thông tin chứ không hoàn toàn tập trung vào việc tư vấn.
               Nếu người dùng trả lời ngắn gọn như "có", "ok", "muốn biết thêm", "tiếp tục", v.v... hãy tiếp tục dựa trên sản phẩm đã được đề cập gần nhất trong lịch sử hội thoại. Đừng tự suy diễn sai sang sản phẩm khác.
               Nếu sản phẩm không có biến thể hay không có các thông tin như sách hàng muốn cung cấp hãy trả lời thẳn thắng rằng bạn chưa tìm thấy thông tin đó
               Nếu người dùng xác nhận hoặc yêu cầu tiếp tục nhưng không nói rõ sản phẩm mới, bạn hãy tiếp tục tư vấn dựa trên sản phẩm đã nhắc đến gần nhất: "%s"
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
                
                Danh sách kích thước:
                %s
                
                Lịch sử hội thoại gần nhất:
                %s
                
                Lịch sử hội thoại gần nhất:
                %s
                
                Sản phẩm được đề cập gần nhất
                %s

                Người dùng hiện tại hỏi: "%s"

                """.formatted(
                lastUserMentionedProduct != null ? lastUserMentionedProduct : "Không xác định",
                productInfo,
                variantPrompt,
                colorPrompt,
                imagePrompt,
                SizePrompt,
                flashSalePrompt,
                focusProductContext,
                historyContext,
                request.getPrompt());
        // Build request JSON
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", fullPrompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(contentPart));
        contents.add(content);
        requestBody.put("contents", contents);
        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        try {
            String fullUrl = GEMINI_URL + "?key=" + apiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, entity, Map.class);
            List candidates = (List) response.getBody().get("candidates");

            if (candidates != null && !candidates.isEmpty()) {

                Map firstCandidate = (Map) candidates.get(0);

                Map contentMap = (Map) firstCandidate.get("content");

                List parts = (List) contentMap.get("parts");

                if (parts != null && !parts.isEmpty()) {

                    Map part = (Map) parts.get(0);

                    String text = (String) part.get("text");

                    return new GeminiResponse(text);

                }

            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        return new GeminiResponse("Không nhận được phản hồi từ Gemini API.");
    }

      //công thức thuốc bổ não
      private String extractLastMentionedProduct(List<ChatTurn> history) {
          if (history == null || history.isEmpty()) return null;

          Map<String, String> normalizedProductNames = productService.findAll().stream()
                  .collect(Collectors.toMap(
                          p -> normalize(p.getName()),
                          ProductResponseDTO::getName
                  ));

          String lastFound = null;

          for (ChatTurn turn : history) {
              String message = turn.getMessage();
              if (message == null) continue;

              String normalizedMessage = normalize(message);
              if ("user".equalsIgnoreCase(turn.getRole()) && isGenericConfirmation(normalizedMessage)) continue;

              Set<String> messageWords = new HashSet<>(Arrays.asList(normalizedMessage.split(" ")));
              for (Map.Entry<String, String> entry : normalizedProductNames.entrySet()) {
                  Set<String> productWords = new HashSet<>(Arrays.asList(entry.getKey().split(" ")));
                  productWords.retainAll(messageWords);
                  if (!productWords.isEmpty()) {
                      lastFound = entry.getValue(); // cập nhật tên sản phẩm gốc
                  }
              }
          }

          return lastFound;
      }


    private boolean isGenericConfirmation(String message) {
        return List.of("co", "ok", "tiep tuc", "muon biet them", "xem them",
                        "dung", "chuan", "phai", "dung roi").
                contains(message);

    }
    private String normalize(String input) {

        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // bỏ dấu tiếng Việt
                .replaceAll("[^a-zA-Z0-9 ]", "") // bỏ ký tự đặc biệt
                .toLowerCase()
                .trim();

    }

}