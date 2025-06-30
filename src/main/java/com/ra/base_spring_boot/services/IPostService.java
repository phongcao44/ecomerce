package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.PostRequestDTO;
import com.ra.base_spring_boot.dto.resp.PostResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostService {

    PostResponseDTO createPost(Long adminId, PostRequestDTO dto);


    PostResponseDTO updatePost(Long id, PostRequestDTO dto);

    void deletePost(Long id);

    List<PostResponseDTO> getAllPosts();

    PostResponseDTO getPostById(Long id);

    List<PostResponseDTO> getAllVisiblePosts();

    List<PostResponseDTO> getRelatedPosts(Long postId);

}
