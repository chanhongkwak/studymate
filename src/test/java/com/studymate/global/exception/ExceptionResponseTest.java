package com.studymate.global.exception;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExceptionResponseTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void exceptionsProduceTheirStatusAndMessage() throws Exception {
        int[] statuses = {400, 401, 403, 404, 409};
        String[] codes = {"BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "CONFLICT"};
        for (int i = 0; i < statuses.length; i++) {
            mvc.perform(get("/errors/" + statuses[i]))
                    .andExpect(status().is(statuses[i]))
                    .andExpect(jsonPath("$.code").value(codes[i]))
                    .andExpect(jsonPath("$.message").value("검증 메시지"));
        }
    }

    @Test
    void malformedUuidAndJsonProduceSafeBadRequest() throws Exception {
        mvc.perform(get("/ids/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mvc.perform(post("/body").contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/errors/{status}")
        void fail(@PathVariable("status") int status) {
            throw switch (status) {
                case 401 -> new UnauthorizedException("검증 메시지");
                case 403 -> new ForbiddenException("검증 메시지");
                case 404 -> new NotFoundException("검증 메시지");
                case 409 -> new ConflictException("검증 메시지");
                default -> new IllegalArgumentException("검증 메시지");
            };
        }

        @GetMapping("/ids/{id}")
        void id(@PathVariable("id") UUID id) {}

        @PostMapping("/body")
        void body(@RequestBody ErrorResponse body) {}
    }
}
