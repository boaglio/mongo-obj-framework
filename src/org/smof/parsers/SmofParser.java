package org.smof.parsers;

import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonValue;
import org.smof.collection.SmofDispatcher;
import org.smof.element.Element;
import org.smof.exception.InvalidBsonTypeException;
import org.smof.exception.InvalidSmofTypeException;
import org.smof.exception.InvalidTypeException;
import org.smof.exception.SmofException;
import org.smof.field.MasterField;
import org.smof.field.PrimaryField;
import org.smof.field.SmofField;

@SuppressWarnings("javadoc")
public class SmofParser {
	
	private static void handleError(Throwable cause) {
		throw new SmofException(cause);
	}
	
	private final SmofTypeContext context;
	private final SmofParserPool parsers;
	private final SmofDispatcher dispatcher;
	
	public SmofParser(SmofDispatcher dispatcher) {
		this.context = new SmofTypeContext();
		parsers = SmofParserPool.create(this);
		this.dispatcher = dispatcher;
	}
	
	SmofTypeContext getContext() {
		return context;
	}
	
	public <T> AnnotationParser<T> getMetadata(Class<T> type) {
		return context.getMetadata(type);
	}
	
	public <T> AnnotationParser<T> registerType(Class<T> type) {
		try {
			final AnnotationParser<T> parser = new AnnotationParser<>(type);
			return registerType(parser);
		} catch (InvalidSmofTypeException e) {
			handleError(e);
			return null;
		}
	}
	
	public <T> AnnotationParser<T> registerType(Class<T> type, Object factory){
		try {
			final AnnotationParser<T> parser = new AnnotationParser<>(type, factory);
			return registerType(parser);
		} catch (InvalidSmofTypeException e) {
			handleError(e);
			return null;
		}
	}
	
	private <T> AnnotationParser<T> registerType(AnnotationParser<T> parser) {
		validateParserFields(parser);
		context.put(parser);
		return parser;
	}
	
	private void validateParserFields(AnnotationParser<?> parser) {
		for(PrimaryField field : parser.getAllFields()) {
			checkValidType(field);
		}
	}
	
	private void checkValidType(SmofField field) {
		final BsonParser parser = parsers.get(field.getType());
		if(!parser.isValidType(field)) {
			handleError(new InvalidTypeException(field.getFieldClass(), field.getType()));
		}
	}

	public <T extends Element> T fromBson(BsonDocument document, Class<T> type) {
		final BsonParser parser = parsers.get(SmofType.OBJECT);
		final MasterField field = new MasterField(type);
		return parser.fromBson(document, type, field);
	}
	
	Object fromBson(BsonValue value, SmofField field) {
		checkValidBson(value, field);
		final BsonParser parser = parsers.get(field.getType());
		final Class<?> type = field.getFieldClass();
		
		return fromBson(parser, value, type, field);
	}
	
	private Object fromBson(BsonParser parser, BsonValue value, Class<?> type, SmofField field) {
		if(value.isNull()) {
			return null;
		}
		return parser.fromBson(value, type, field);
	}
	
	public BsonDocument toBson(Element value) {
		final BsonParser parser = parsers.get(SmofType.OBJECT);
		final MasterField field = new MasterField(value.getClass());
		return (BsonDocument) parser.toBson(value, field);
	}

	BsonValue toBson(Object value, SmofField field) {
		if(value == null) {
			return new BsonNull();
		}
		final SmofType type = field.getType();
		final BsonParser parser = parsers.get(type);
		return parser.toBson(value, field);
	}

	private void checkValidBson(BsonValue value, SmofField field) {
		final BsonParser parser = parsers.get(field.getType());
		if(!parser.isValidBson(value)) {
			handleError(new InvalidBsonTypeException(value.getBsonType(), field.getName()));
		}
	}

	boolean isValidType(SmofField field) {
		final BsonParser parser = parsers.get(field.getType());
		return parser.isValidType(field.getFieldClass());
	}
}
