package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ContactFormRequest;
import com.ra.base_spring_boot.model.Contact;

import java.util.List;

public interface IContactService {
    Contact create(long userId,ContactFormRequest contact);
    List<Contact> findAll();
    Contact findById(long id);
    void delete(long id);
}
