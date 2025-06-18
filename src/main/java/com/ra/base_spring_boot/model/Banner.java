package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "banners")
public class Banner extends BaseObject {

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "position")
    private String position;
}
