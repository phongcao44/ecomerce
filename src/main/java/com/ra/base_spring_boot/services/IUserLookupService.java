package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.User;

public interface IUserLookupService {
    User findUser(long userId);
}
