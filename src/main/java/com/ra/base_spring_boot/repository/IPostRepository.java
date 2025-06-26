package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPostRepository extends JpaRepository<Post, Long> {
    List<Post> findTop4ByLocationAndIdNot(String location, Long id);

}
