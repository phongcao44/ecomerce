    package com.ra.base_spring_boot.services.impl;

    import com.cloudinary.Cloudinary;
    import com.cloudinary.utils.ObjectUtils;
    import com.ra.base_spring_boot.dto.req.ReturnRequestDTO;
    import com.ra.base_spring_boot.dto.resp.OrderItemDetailDTO;
    import com.ra.base_spring_boot.dto.resp.ReturnRequestItemResponseDTO;
    import com.ra.base_spring_boot.dto.resp.ReturnRequestResponseDTO;
    import com.ra.base_spring_boot.exception.HttpBadRequest;
    import com.ra.base_spring_boot.exception.HttpNotFound;
    import com.ra.base_spring_boot.model.*;
    import com.ra.base_spring_boot.model.constants.OrderStatus;
    import com.ra.base_spring_boot.model.constants.ReturnStatus;
    import com.ra.base_spring_boot.repository.IOrderItemRepository;
    import com.ra.base_spring_boot.repository.IOrderRepository;
    import com.ra.base_spring_boot.repository.IReturnRequestRepository;
    import com.ra.base_spring_boot.services.IProductVariantService;
    import com.ra.base_spring_boot.services.IReturnRequestService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Map;

    @Service
    @RequiredArgsConstructor
    public class ReturnRequestServiceImpl implements IReturnRequestService {

        private final IReturnRequestRepository returnRequestRepository;

        private final IOrderRepository orderRepository;

        private final Cloudinary cloudinary;

        private final IOrderItemRepository orderItemRepository;
        private String uploadMedia(MultipartFile file) {
            try {
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "returns",
                                "resource_type", "auto" // Cho phép cả ảnh và video
                        )
                );
                return (String) uploadResult.get("secure_url");
            } catch (IOException e) {
                throw new RuntimeException("Upload media failed", e);
            }
        }

        @Override
        public List<ReturnRequestResponseDTO> getAll() {
            List<ReturnRequest> requests = returnRequestRepository.findAll();
            return requests.stream()
                    .map(r -> ReturnRequestResponseDTO.builder()
                            .id(r.getId())
                            .orderId(r.getOrder().getId())
                            .reason(r.getReason())
                            .mediaUrl(r.getMediaUrl())
                            .status(r.getStatus())
                            .createdAt(r.getCreatedAt())
                            .fullName(r.getUser().getUsername())
                            .build())
                    .toList();
        }


        @Override
        public ReturnRequestResponseDTO getDetailById(Long id) {
            ReturnRequest request = returnRequestRepository.findById(id)
                    .orElseThrow(() -> new HttpNotFound("Không tìm thấy yêu cầu trả hàng"));

            Order order = request.getOrder();

            List<OrderItemDetailDTO> items = order.getOrderItems().stream()
                    .map(OrderItemDetailDTO::fromOrderItem)
                    .toList();

            return ReturnRequestResponseDTO.builder()
                    .id(request.getId())
                    .orderId(order.getId())
                    .reason(request.getReason())
                    .mediaUrl(request.getMediaUrl())
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .fullName(request.getUser().getUsername())
                    .items(items)
                    .build();
        }

        @Override
        public void updateStatus(Long id, ReturnStatus status) {
            ReturnRequest request = returnRequestRepository.findById(id)
                    .orElseThrow(() -> new HttpNotFound("Không tìm thấy yêu cầu trả hàng"));

            if (request.getStatus() == ReturnStatus.APPROVED) {
                throw new HttpBadRequest("Yêu cầu này đã hoàn tất trả hàng");
            }

            request.setStatus(status);
            returnRequestRepository.save(request);
        }

        @Override
        public ReturnRequestResponseDTO create(ReturnRequestDTO dto, User user) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new HttpNotFound("Không tìm thấy đơn hàng"));

            if (!order.getUser().getId().equals(user.getId())) {
                throw new HttpBadRequest("Bạn không có quyền gửi yêu cầu cho đơn này");
            }

            if (dto.getMedia() == null || dto.getMedia().isEmpty()) {
                throw new HttpBadRequest("Vui lòng chọn ảnh/video minh chứng");
            }

            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new HttpBadRequest("Chỉ có thể yêu cầu trả hàng với đơn hàng đã giao");
            }
            OrderItem orderItem = orderItemRepository.findById(dto.getItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong đơn hàng"));
            String mediaUrl = uploadMedia(dto.getMedia());

            ReturnRequest request = ReturnRequest.builder()
                    .user(user)
                    .order(order)
                    .orderItem(orderItem)
                    .reason(dto.getReason())
                    .mediaUrl(mediaUrl)
                    .status(ReturnStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            returnRequestRepository.save(request);

            return ReturnRequestResponseDTO.builder()
                    .id(request.getId())
                    .orderId(order.getId())
                    .reason(request.getReason())
                    .mediaUrl(request.getMediaUrl())
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .build();
        }

        @Override
        public List<ReturnRequestItemResponseDTO> getByUser(User user) {
            List<ReturnRequest> requests = returnRequestRepository.findByUserId(user.getId());

            return requests.stream()
                    .map(r -> {
                        OrderItem item = r.getOrderItem(); // returnRequest liên kết trực tiếp với 1 OrderItem

                        return ReturnRequestItemResponseDTO.builder()
                                .id(r.getId())
                                .orderId(item.getOrder().getId())
                                .reason(r.getReason())
                                .mediaUrl(r.getMediaUrl())
                                .status(r.getStatus())
                                .createdAt(r.getCreatedAt())
                                .productName(item.getVariant().getProduct().getName())
                                .price(item.getPriceAtTime())
                                .fullName(user.getUsername())
                                .build();
                    })
                    .toList();
        }


        @Override
        public ReturnRequestResponseDTO getById(Long id, User user) {
            ReturnRequest returned = returnRequestRepository.findById(id)
                    .orElseThrow(() -> new HttpNotFound("Không tìm thấy yêu cầu"));

            if (!returned.getUser().getId().equals(user.getId())) {
                throw new HttpBadRequest("Bạn không có quyền truy cập vào để xem yêu cầu trả đơn hàng này");
            }

            return ReturnRequestResponseDTO.builder()
                    .id(returned.getId())
                    .orderId(returned.getOrder().getId())
                    .reason(returned.getReason())
                    .mediaUrl(returned.getMediaUrl())
                    .status(returned.getStatus())
                    .createdAt(returned.getCreatedAt())
                    .build();
        }
    }
