package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ReturnRequestDTO;
import com.ra.base_spring_boot.dto.resp.ReturnRequestItemResponseDTO;
import com.ra.base_spring_boot.dto.resp.ReturnRequestResponseDTO;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ReturnStatus;

import java.util.List;

public interface IReturnRequestService {

    List<ReturnRequestResponseDTO> getAll();

    ReturnRequestResponseDTO getDetailById(Long id);

    void updateStatus(Long id, ReturnStatus status);

    ReturnRequestResponseDTO create(ReturnRequestDTO dto, User user);

    List<ReturnRequestItemResponseDTO> getByUser(User user);

    ReturnRequestResponseDTO getById(Long id, User user);
}
