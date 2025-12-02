package com.example.angella.eventsplatform.service.checker;

import com.example.angella.eventsplatform.aop.AccessCheckType;
import com.example.angella.eventsplatform.aop.AccessAnnotation;
import jakarta.servlet.http.HttpServletRequest;

public interface AccessCheckerService {

    boolean check(HttpServletRequest request, AccessAnnotation accessible);

    AccessCheckType getType();

}
