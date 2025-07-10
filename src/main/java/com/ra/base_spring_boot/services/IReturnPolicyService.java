package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ReturnPolicyRequestDTO;
import com.ra.base_spring_boot.dto.resp.ReturnPolicyResponseDTO;
import com.ra.base_spring_boot.model.User;

import java.util.List;

public interface IReturnPolicyService {

    ReturnPolicyResponseDTO create(ReturnPolicyRequestDTO dto, User admin);

    ReturnPolicyResponseDTO update(Long id, ReturnPolicyRequestDTO dto, User admin);

    List<ReturnPolicyResponseDTO> getAll();

    void delete(Long id, User admin);

    ReturnPolicyResponseDTO getById(Long id);

}

