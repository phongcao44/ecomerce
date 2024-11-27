package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.JwtResponse;
import com.ra.base_spring_boot.exception.CustomException;

public interface IAuthService
{

    void register(FormRegister formRegister) throws CustomException;

    JwtResponse login(FormLogin formLogin) throws CustomException;

}
