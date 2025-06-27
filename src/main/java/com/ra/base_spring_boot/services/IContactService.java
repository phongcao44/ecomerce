package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ContactFormRequest;
import com.ra.base_spring_boot.model.Contact;

public interface IContactService {
    Contact create(long userId,ContactFormRequest contact);

}
