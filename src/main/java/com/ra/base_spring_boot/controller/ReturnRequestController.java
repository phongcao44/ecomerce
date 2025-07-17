    package com.ra.base_spring_boot.controller;

    import com.ra.base_spring_boot.dto.req.ReturnRequestDTO;
    import com.ra.base_spring_boot.dto.req.UpdateReturnStatusRequest;
    import com.ra.base_spring_boot.dto.resp.ReturnRequestResponseDTO;
    import com.ra.base_spring_boot.security.principle.MyUserDetails;
    import com.ra.base_spring_boot.services.IReturnRequestService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/api/v1")
    @RequiredArgsConstructor
    public class ReturnRequestController {

        private final IReturnRequestService returnRequestService;

        // Danh sách đổi trả Admin
        @GetMapping("/admin/return-request/list")
            public ResponseEntity<List<ReturnRequestResponseDTO>> list() {
                return ResponseEntity.ok(returnRequestService.getAll());
        }

        // Xem chi tiết yêu cầu đổi trả ngươ dùng
        @GetMapping("/admin/return-request/{id}")
        public ResponseEntity<ReturnRequestResponseDTO> get(@PathVariable Long id) {
            return ResponseEntity.ok(returnRequestService.getDetailById(id));
        }

        // Gửi yêu cầu đổi/trả
        @PostMapping(value = "/user/return-request/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> create(@ModelAttribute ReturnRequestDTO dto,
                                        @AuthenticationPrincipal MyUserDetails userDetails) {
            ReturnRequestResponseDTO response = returnRequestService.create(dto, userDetails.getUser());
            return ResponseEntity.ok(response);
        }

        @PutMapping("/admin/return-request/update/{id}")
        public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                              @RequestBody UpdateReturnStatusRequest request) {
            returnRequestService.updateStatus(id, request.getStatus());
            return ResponseEntity.ok("Cập nhật trạng thái thành công");
        }


        // Lấy danh sách yêu cầu của người dùng
        @GetMapping("/user/my-requests/list")
        public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal MyUserDetails userDetails) {
            return ResponseEntity.ok(returnRequestService.getByUser(userDetails.getUser()));
        }

        // Xem chi tiết yêu cầu
        @GetMapping("/user/return-request/{id}")
        public ResponseEntity<?> getDetail(@PathVariable Long id,
                                           @AuthenticationPrincipal MyUserDetails userDetails) {
            return ResponseEntity.ok(returnRequestService.getById(id, userDetails.getUser()));
        }
    }