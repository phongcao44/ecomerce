package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.req.PostRequestDTO;
import com.ra.base_spring_boot.dto.resp.PostResponseDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1")
public class PostController {
    @Autowired
    private IPostService postService;

    @PostMapping(value = "/admin/posts/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDTO> createPost(@AuthenticationPrincipal MyUserDetails userDetails,
            @ModelAttribute PostRequestDTO request
    ) {
        Long adminId = userDetails.getUser().getId();
        return ResponseEntity.ok(postService.createPost(adminId, request));
    }


    @PutMapping(value = "/admin/posts/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDTO> updatePost(@PathVariable Long id, @ModelAttribute PostRequestDTO dto) {
        PostResponseDTO response = postService.updatePost(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/posts/delete/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok("Post Deleted Successfully");
    }

    @GetMapping("/admin/posts/list")
    public ResponseEntity<List<PostResponseDTO>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/admin/posts/{id}")
    public ResponseEntity<PostResponseDTO> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    // Danh sách bài viết
    @GetMapping("/posts/list")
    public ResponseEntity<List<PostResponseDTO>> getAll() {
        return ResponseEntity.ok(postService.getAllVisiblePosts());
    }

    // Chi tiết bài viết
    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponseDTO> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    // Bài viết liên quan
    @GetMapping("/posts/{id}/related")
    public ResponseEntity<List<PostResponseDTO>> getRelated(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getRelatedPosts(id));
    }
}
