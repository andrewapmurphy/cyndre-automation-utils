package com.cyndre.test.rule;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

public class TestExceptionMapper {
	@Rule
	public final ApplyToExceptionRule mapException = ApplyToExceptionRule.prependExceptionMessage(new ExceptionMessageGenerator() {
		public String generateMessage(Throwable t) {
			return "This message is prefixed to any exception thrown and can include information like what page we're on - ";
		}
	});
	
	@Test(expected=Exception.class)
	public void doTest() {
		assertEquals("1=2", 1, 2);
	}
}
