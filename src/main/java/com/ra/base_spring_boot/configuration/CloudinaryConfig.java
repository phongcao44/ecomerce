package com.ra.base_spring_boot.configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dai69djx3",
                "api_key", "317635233937892",
                "api_secret", "IPqmezt5RlPDgxx6mxwNdhn9SIg",
                "secure", true
        ));
    }
}

