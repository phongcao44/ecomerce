package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.ICompareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compare")
@RequiredArgsConstructor
public class CompareController {

    private final ICompareService compareService;

    @PostMapping("/user/add/{productId}")
    public ResponseEntity<?> add(@PathVariable Long productId,
                                 @AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(compareService.addToCompare(userDetails.getUser().getId(), productId));
    }

    @DeleteMapping("/user/remove/{productId}")
    public ResponseEntity<?> remove(@PathVariable Long productId,
                                    @AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(compareService.removeFromCompare(userDetails.getUser().getId(), productId));
    }

    @GetMapping("/user/list")
    public ResponseEntity<?> list(@AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(compareService.getCompareList(userDetails.getUser().getId()));
    }
}
