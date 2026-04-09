package com.hoangpt.spring_security.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity //active spring web security
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        // Sử dụng BCrypt để mã hóa mật khẩu thay vì để dạng plain text hoặc {noop}
        return new BCryptPasswordEncoder();
    }


    /**
     * Config User Information
     */
    @Bean
    public UserDetailsService userDetailsService() {
//        UserDetails user = User.withUsername("user")
//                .password("123") //raw
//                .roles("USER")
//                .build();
//
//        UserDetails admin = User.withUsername("admin")
//                .password("{noop}123") //raw no encode
//                .roles("USER", "ADMIN")
//                .build();
        UserDetails user = User.withUsername("user")
                .password(passwordEncoder().encode("123"))
                .roles("USER") // gán vai trò, tự động thêm PREFIX: ROLE_
                .authorities("ROLE_USER")
                .build();

        // Khởi tạo Admin (đã sửa lỗi gọi hàm passwordEncoder)
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder().encode("123"))
                .roles("USER", "ADMIN")
                .authorities("ROLE_USER", "ROLE_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);

    }
}
