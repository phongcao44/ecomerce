package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContactRepository extends JpaRepository<Contact,Long> {

}
