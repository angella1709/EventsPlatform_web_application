package com.example.angella.eventsplatform.utils;

import com.example.angella.eventsplatform.exception.ServerException;
import com.example.angella.eventsplatform.security.AppUserDetails;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@UtilityClass
public class AuthUtils {

    public static Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails instanceof AppUserDetails details) {
            return details.getId();
        }

        throw new ServerException("UserDetails is not an instance of AppUserDetails");
    }

    public static AppUserDetails getAuthenticatedUser() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof AppUserDetails details) {
            return details;
        }

        throw new ServerException("The principal object in the security context is not an instance of AppUserDetails");
    }
}
