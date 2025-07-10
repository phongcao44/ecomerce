package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ReturnPolicyRequestDTO;
import com.ra.base_spring_boot.dto.resp.ReturnPolicyResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.ReturnPolicy;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IReturnPolicyRepository;
import com.ra.base_spring_boot.services.IReturnPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnPolicyServiceImpl implements IReturnPolicyService {

    private final IReturnPolicyRepository returnPolicyRepository;

    @Override
    public ReturnPolicyResponseDTO create(ReturnPolicyRequestDTO dto, User admin) {
        ReturnPolicy policy = ReturnPolicy.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .returnDays(dto.getReturnDays())
                .allowReturnWithoutReason(dto.getAllowReturnWithoutReason())
                .status(dto.getStatus())
                .admin(admin)
                .build();
        returnPolicyRepository.save(policy);
        return ReturnPolicyResponseDTO.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .content(policy.getContent())
                .returnDays(policy.getReturnDays())
                .allowReturnWithoutReason(policy.getAllowReturnWithoutReason())
                .status(policy.getStatus())
                .adminName(admin.getUsername())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();

    }

    @Override
    public ReturnPolicyResponseDTO update(Long id, ReturnPolicyRequestDTO dto, User admin) {
        ReturnPolicy policy = returnPolicyRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Không tìm thấy chính sách"));

        if (!policy.getAdmin().getId().equals(admin.getId())) {
            throw new HttpBadRequest("Bạn không có quyền chỉnh sửa chính sách này");
        }

        policy.setTitle(dto.getTitle());
        policy.setContent(dto.getContent());
        policy.setReturnDays(dto.getReturnDays());
        policy.setAllowReturnWithoutReason(dto.getAllowReturnWithoutReason());
        policy.setStatus(dto.getStatus());
        returnPolicyRepository.save(policy);

        return ReturnPolicyResponseDTO.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .content(policy.getContent())
                .returnDays(policy.getReturnDays())
                .allowReturnWithoutReason(policy.getAllowReturnWithoutReason())
                .status(policy.getStatus())
                .adminName(admin.getUsername())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    @Override
    public List<ReturnPolicyResponseDTO> getAll() {
        return returnPolicyRepository.findAll().stream()
                .map(policy -> ReturnPolicyResponseDTO.builder()
                        .id(policy.getId())
                        .title(policy.getTitle())
                        .content(policy.getContent())
                        .returnDays(policy.getReturnDays())
                        .allowReturnWithoutReason(policy.getAllowReturnWithoutReason())
                        .status(policy.getStatus())
                        .adminName(policy.getAdmin().getUsername())
                        .createdAt(policy.getCreatedAt())
                        .updatedAt(policy.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id, User admin) {
        ReturnPolicy policy = returnPolicyRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Không tìm thấy chính sách"));

        if (!policy.getAdmin().getId().equals(admin.getId())) {
            throw new HttpBadRequest("Bạn không có quyền xóa chính sách này");
        }

        returnPolicyRepository.delete(policy);
    }

    @Override
    public ReturnPolicyResponseDTO getById(Long id) {
        ReturnPolicy policy = returnPolicyRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Không tìm thấy chính sách"));

        return ReturnPolicyResponseDTO.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .content(policy.getContent())
                .returnDays(policy.getReturnDays())
                .allowReturnWithoutReason(policy.getAllowReturnWithoutReason())
                .status(policy.getStatus())
                .adminName(policy.getAdmin().getUsername())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}