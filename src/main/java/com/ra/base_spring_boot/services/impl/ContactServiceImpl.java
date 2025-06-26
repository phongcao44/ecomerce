package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ContactFormRequest;
import com.ra.base_spring_boot.email.EmailService;
import com.ra.base_spring_boot.model.Contact;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IAddressRepository;
import com.ra.base_spring_boot.repository.IContactRepository;
import com.ra.base_spring_boot.services.IContactService;
import com.ra.base_spring_boot.services.IUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactServiceImpl implements IContactService {
    private final EmailService emailService;
    private final IUserService userService;
    private final IContactRepository contactRepository;
    public ContactServiceImpl(EmailService emailService, IUserService userService, IContactRepository contactRepository) {
        this.emailService = emailService;
        this.userService = userService;
        this.contactRepository = contactRepository;
    }

    @Override
    public Contact create(long userId, ContactFormRequest contact) {
        User user = userService.findUser(userId);
        Contact newContact = Contact.builder().
                email(contact.getEmail()).
                name(contact.getName()).
                phone(contact.getPhone()).
                createdAt(LocalDateTime.now()).
                user(user).
                message(contact.getMessage()).
                build();
        contactRepository.save(newContact);
        emailService.sendContactEmail(contact);

        return newContact;
    }

    @Override
    public List<Contact> findAll() {
        return contactRepository.findAll();
    }

    @Override
    public Contact findById(long id) {
        return contactRepository.findById(id).orElse(null);
    }

    @Override
    public void delete(long id) {
        contactRepository.deleteById(id);
    }

}
