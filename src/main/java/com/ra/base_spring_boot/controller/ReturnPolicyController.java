package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.ReturnPolicyRequestDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IReturnPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReturnPolicyController {

    private final IReturnPolicyService returnPolicyService;

    @GetMapping("/return-policy/list")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(returnPolicyService.getAll());
    }

    @PostMapping("/admin/return-policy/add")
    public ResponseEntity<?> create(@RequestBody ReturnPolicyRequestDTO dto,
                                    @AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(returnPolicyService.create(dto, userDetails.getUser()));
    }

    @PutMapping("/admin/return-policy/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody ReturnPolicyRequestDTO dto,
                                    @AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(returnPolicyService.update(id, dto, userDetails.getUser()));
    }

    @DeleteMapping("/admin/return-policy/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal MyUserDetails userDetails) {
        returnPolicyService.delete(id, userDetails.getUser());
        return ResponseEntity.ok("Xóa chính sách thành công");
    }

    @GetMapping("/return-policy/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(returnPolicyService.getById(id));
    }

}
