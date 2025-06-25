package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPostRepository extends JpaRepository<Post, Long> {
}
