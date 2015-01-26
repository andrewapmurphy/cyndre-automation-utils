package com.cyndre.test.rule;

import java.lang.reflect.Constructor;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Rule for applying a conversion to an exception before reporting it
 */
public class ApplyToExceptionRule implements TestRule {
	/**
	 * Interface for converting one exception to another
	 */
	public static interface ExceptionConverter {
		public Throwable convert(final Throwable t);
	}

	/**
	 * Creates a new copy of an exception and prepends a message
	 */
	public static class PrependMessageExceptionConverter implements ExceptionConverter {
		private final ExceptionMessageGenerator generator;
		
		public PrependMessageExceptionConverter(final ExceptionMessageGenerator generator) {
			this.generator = generator;
		}


		public Throwable convert(Throwable t) {
			final Class<? extends Throwable> originalExceptionClass = t.getClass();
			
			final String newMessage = generator.generateMessage(t) + t.getMessage();
			
			try {
				//first try finding a constructor for the exception that can take a message and root cause.
				final Constructor<? extends Throwable> messageAndCauseConstructor = originalExceptionClass.getConstructor(String.class, Throwable.class);

				final Throwable newException = messageAndCauseConstructor.newInstance(newMessage, t.getCause());
				newException.setStackTrace(t.getStackTrace()); //preserve stack trace
				
				return newException;
			} catch (NoSuchMethodException noMessageAndCauseException) {
				try {
					final Constructor<? extends Throwable> messageConstructor = originalExceptionClass.getConstructor(String.class);

					final Throwable newException = messageConstructor.newInstance(newMessage);
					newException.setStackTrace(t.getStackTrace()); //preserve stack trace
					
					return newException;
				} catch (Exception e) {
					//no constructor that takes just a message (we can't prepend anything) or something else went wrong, just return the original exception
					return t;
				}
			} catch (Exception e) {
				//couldn't find a constructor or something else went wrong, just return the original exception
				return t;
			}
		}
	}
	
	/**
	 * Creates a new instance of a PrependMessageExceptionConverter
	 * 
	 * @see PrependMessageExceptionConverter
	 */
	public static ApplyToExceptionRule prependExceptionMessage(final ExceptionMessageGenerator generator) {
		return new ApplyToExceptionRule(
			new PrependMessageExceptionConverter(generator)
		);
	}
	
	private final ExceptionConverter exceptionMapper;
	
	public ApplyToExceptionRule(final ExceptionConverter mapper) {
		this.exceptionMapper = mapper;
	}

	/**
	 * Apply this rule to the statement that's about to be executed
	 */
	public Statement apply(Statement paramStatement, Description paramDescription) {
		return new MapExceptionRuleStatement(paramStatement);
	}
	
	/**
	 * Executes a statement (usually a test) and converts any exceptions that occur
	 */
	private class MapExceptionRuleStatement extends Statement {
		private final Statement nextStatement;
		
		public MapExceptionRuleStatement(final Statement statement) {
			this.nextStatement = statement;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.nextStatement.evaluate();
			} catch (Throwable e) {
				throw exceptionMapper.convert(e);
			}
		}
		
	}
}
