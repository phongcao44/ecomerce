package com.ra.base_spring_boot.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ra.base_spring_boot.model.Banner;
import com.ra.base_spring_boot.repository.IBannerRepository;
import com.ra.base_spring_boot.services.IBannerService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements IBannerService {

    private final IBannerRepository bannerRepository;
    private final Cloudinary cloudinary;

    public BannerServiceImpl(IBannerRepository bannerRepository, Cloudinary cloudinary) {
        this.bannerRepository = bannerRepository;
        this.cloudinary = cloudinary;
    }

  /*  @Override
    public Banner create(String title, String position, Boolean status, MultipartFile image) {
        String imageUrl = uploadImageToCloudinary(image);
        Banner banner = Banner.builder()
                .title(title)
                .position(position)
                .status(status)
                .bannerUrl(imageUrl)
                .build();
        return bannerRepository.save(banner);
    }*/

    /*private String uploadImageToCloudinary(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "banners"));
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }*/
    private Map uploadImageToCloudinary(MultipartFile file) {
        try {
            return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "banners"
            ));
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }
    @Override
    public Banner create(String title, String position, Boolean status, OffsetDateTime startTime, OffsetDateTime  endTime, MultipartFile image) {
        Map uploadResult = uploadImageToCloudinary(image);
        Banner banner = Banner.builder()
                .title(title)
                .position(position)
                .status(status)
                .startAt(startTime)          // <- Cần thêm dòng này
                .endAt(endTime)
                .bannerUrl((String) uploadResult.get("secure_url"))
                .publicId((String) uploadResult.get("public_id"))
                .build();
        return bannerRepository.save(banner);
    }

    public Banner getById(Integer id) {
        return bannerRepository.findById(id).orElseThrow();
    }

    @Override
    public Banner update(Integer id, String title, String position, Boolean status, MultipartFile image) {
        Banner existing = getById(id);

        if (image != null && !image.isEmpty()) {
            // Xoá ảnh cũ
            deleteImageFromCloudinary(existing.getPublicId());

            // Upload ảnh mới
            Map uploadResult = uploadImageToCloudinary(image);
            existing.setBannerUrl((String) uploadResult.get("secure_url"));
            existing.setPublicId((String) uploadResult.get("public_id"));
        }

        //Cập nhật các trường khác
        existing.setTitle(title);
        existing.setPosition(position);
        existing.setStatus(status);

        return bannerRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        Banner banner = getById(id);

        // Xoá ảnh trên Cloudinary
        deleteImageFromCloudinary(banner.getPublicId());

        // Xoá trong database
        bannerRepository.deleteById(id);
    }

    @Override
    public List<Banner> getAll() {
        return bannerRepository.findAll();
    }

    private void deleteImageFromCloudinary(String publicId) {
        try {
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            throw new RuntimeException("Xoá ảnh trên Cloudinary thất bại", e);
        }
    }
    public List<Banner> getAllVisibleBanners() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return bannerRepository.findAll().stream()
                .filter(b -> (b.getStartAt() == null || !now.isBefore(b.getStartAt())) &&
                        (b.getEndAt() == null || !now.isAfter(b.getEndAt())))
                .collect(Collectors.toList());

    }



}
