package com.ra.security;

import com.ra.model.entity.Role;
import com.ra.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserPrinciple implements UserDetails {
    //tao doi tuong
    private User user;
    private Role role;
    //getidcuauser
    public long userId(){
        return user.getId();
    };

    public boolean status(){
        return user.getStatus();
    }

    public void updateStatus(boolean status) {
        user.setStatus(status);
    }

    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    // lấy pass
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    //lấy tên
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    // check xem có fasle ko
    @Override
    public boolean isEnabled() {
        return user.getStatus();
    }
}
