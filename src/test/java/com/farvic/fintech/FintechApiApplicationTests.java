package com.farvic.fintech;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.farvic.fintech.service.IdempotencyService;

@SpringBootTest
@ActiveProfiles("test")
class FintechApiApplicationTests {

	@SuppressWarnings("unused")
	@MockitoBean
	private IdempotencyService idempotencyService;

	@Test
	void contextLoads() {
	}

}
