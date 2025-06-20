package com.ra.base_spring_boot.services;


import com.ra.base_spring_boot.model.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IBannerService {
    Banner update(Integer id, String title, String position, Boolean status, MultipartFile image);
    Banner create(String title, String position, Boolean status, MultipartFile image);
    void delete(Integer id);
    List<Banner> getAll();
}
