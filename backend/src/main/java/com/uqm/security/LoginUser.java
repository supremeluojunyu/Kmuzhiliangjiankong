package com.uqm.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {

    private final Integer userId;
    private final String account;
    private final String password;
    private final String name;
    private final Integer collegeId;
    private final Integer currentGroupId;
    private final List<String> permissions;

    public LoginUser(Integer userId, String account, String password, String name,
                     Integer collegeId, Integer currentGroupId, List<String> permissions) {
        this.userId = userId;
        this.account = account;
        this.password = password;
        this.name = name;
        this.collegeId = collegeId;
        this.currentGroupId = currentGroupId;
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return account;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
