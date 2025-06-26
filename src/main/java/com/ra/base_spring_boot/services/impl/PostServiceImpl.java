package com.ra.base_spring_boot.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ra.base_spring_boot.dto.req.PostRequestDTO;
import com.ra.base_spring_boot.dto.resp.PostResponseDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Post;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IPostRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {

    private final IPostRepository postRepository;

    private final IUserRepository userRepository;

    private final Cloudinary cloudinary;

    private String uploadImage(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "posts"));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Upload image failed", e);
        }
    }


    @Override
    public PostResponseDTO createPost(Long adminId, PostRequestDTO dto) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new HttpNotFound("Admin Not Found"));

        // Lấy file ảnh từ DTO
        MultipartFile imageFile = dto.getImage();
        String imageUrl = uploadImage(imageFile);

        Post post = Post.builder()
                .title(dto.getTitle())
                .image(imageUrl)
                .content(dto.getContent())
                .description(dto.getDescription())
                .location(dto.getLocation())
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        postRepository.save(post);

        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .image(post.getImage())
                .content(post.getContent())
                .description(post.getDescription())
                .location(post.getLocation())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .authorName(user.getUsername())
                .build();
    }


    @Override
    public PostResponseDTO updatePost(Long id, PostRequestDTO dto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Post Not Found"));

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setDescription(dto.getDescription());
        post.setLocation(dto.getLocation());

        // Nếu có ảnh mới => upload và set lại
        MultipartFile imageFile = dto.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = uploadImage(imageFile);
            post.setImage(imageUrl);
        }

        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .image(post.getImage())
                .content(post.getContent())
                .description(post.getDescription())
                .location(post.getLocation())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .authorName(post.getUser().getUsername())
                .build();
    }

    @Override
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Post Not Found"));
        postRepository.delete(post);
    }

    @Override
    public List<PostResponseDTO> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(post -> PostResponseDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .image(post.getImage())
                        .content(post.getContent())
                        .description(post.getDescription())
                        .location(post.getLocation())
                        .createdAt(post.getCreatedAt())
                        .updatedAt(post.getUpdatedAt())
                        .authorName(post.getUser().getUsername())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public PostResponseDTO getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Post Not Found"));
        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .image(post.getImage())
                .content(post.getContent())
                .description(post.getDescription())
                .location(post.getLocation())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .authorName(post.getUser().getUsername())
                .build();
    }

    @Override
    public List<PostResponseDTO> getAllVisiblePosts() {
        return postRepository.findAll()
                .stream()
                .map(post -> PostResponseDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .image(post.getImage())
                        .content(post.getContent())
                        .description(post.getDescription())
                        .location(post.getLocation())
                        .createdAt(post.getCreatedAt())
                        .updatedAt(post.getUpdatedAt())
                        .authorName(post.getUser().getUsername())
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public List<PostResponseDTO> getRelatedPosts(Long postId) {
        Post currentPost = postRepository.findById(postId)
                .orElseThrow(() -> new HttpNotFound("Post Not Found"));

        List<Post> related = postRepository
                .findTop4ByLocationAndIdNot(currentPost.getLocation(), currentPost.getId());

//        if (related.isEmpty()) {
//            throw new HttpNotFound("No Related Posts");
//        }
        return related.stream()
                .map(post -> PostResponseDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .description(post.getDescription())
                        .image(post.getImage())
                        .content(post.getContent())
                        .location(post.getLocation())
                        .createdAt(post.getCreatedAt())
                        .updatedAt(post.getUpdatedAt())
                        .authorName(post.getUser().getUsername())
                        .build())
                .collect(Collectors.toList());
    }
}

