package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TransferApiSecurityIntegrationTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void superAdminRetainsTransferReviewAndApprovalButNotCreateOrCancel() {
        var transferController = handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName().equals("com.pms.api.transfer.controller"))
                .toList();

        assertTrue(transferController.size() >= 5);

        assertPreAuthorize("list", "SUPER_ADMIN");
        assertPreAuthorize("approve", "SUPER_ADMIN");
        assertPreAuthorize("reject", "SUPER_ADMIN");

        assertPreAuthorize("create", "RECORDS_OFFICER");
        assertPreAuthorize("cancel", "RECORDS_OFFICER");
        assertPreAuthorize("create", "SUPER_ADMIN", false);
        assertPreAuthorize("cancel", "SUPER_ADMIN", false);
    }

    private void assertPreAuthorize(String methodName, String roleName) {
        assertPreAuthorize(methodName, roleName, true);
    }

    private void assertPreAuthorize(String methodName, String roleName, boolean required) {
        Method method = findMethod(methodName);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, methodName + " must declare @PreAuthorize.");
        String expression = preAuthorize.value();
        if (required) {
            assertTrue(expression.contains(roleName), methodName + " must include " + roleName + ": " + expression);
        } else {
            assertTrue(!expression.contains(roleName), methodName + " must not include " + roleName + ": " + expression);
        }
    }

    private Method findMethod(String methodName) {
        return handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName().equals("com.pms.api.transfer.controller"))
                .map(handler -> handler.getMethod())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No transfer method named " + methodName));
    }
}
