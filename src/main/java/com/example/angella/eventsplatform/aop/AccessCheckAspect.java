package com.example.angella.eventsplatform.aop;

import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.service.checker.AccessCheckerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AccessCheckAspect {

    private final Map<AccessCheckType, AccessCheckerService> accessCheckServiceMap;

    @Before("@annotation(accessAnnotation)")
    public void validateAccessPermissions(JoinPoint joinPoint, AccessAnnotation accessAnnotation) {
        validateAnnotationPresence(accessAnnotation);

        HttpServletRequest currentRequest = extractCurrentHttpRequest();
        AccessCheckerService accessChecker = resolveAccessChecker(accessAnnotation.checkBy());

        if (!accessChecker.check(currentRequest, accessAnnotation)) {
            handleAccessDenied(joinPoint);
        }
    }

    private void validateAnnotationPresence(AccessAnnotation annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Access annotation must not be null");
        }
    }

    private HttpServletRequest extractCurrentHttpRequest() {
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            throw new IllegalStateException("No active HTTP request found");
        }

        return requestAttributes.getRequest();
    }

    private AccessCheckerService resolveAccessChecker(AccessCheckType checkType) {
        AccessCheckerService checker = accessCheckServiceMap.get(checkType);
        if (checker == null) {
            throw new IllegalArgumentException(
                    String.format("No AccessCheckerService found for type: %s", checkType)
            );
        }
        return checker;
    }

    private void handleAccessDenied(JoinPoint joinPoint) {
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        log.warn("Access violation detected for method: {}", methodName);
        throw new AccessDeniedException("Insufficient permissions to perform this operation");
    }
}