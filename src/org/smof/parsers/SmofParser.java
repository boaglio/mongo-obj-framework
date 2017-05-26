/*******************************************************************************
 * Copyright (C) 2017 Joao
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.smof.parsers;

import java.util.Set;

import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonValue;
import org.bson.types.ObjectId;
import org.smof.collection.SmofDispatcher;
import org.smof.element.Element;
import org.smof.exception.InvalidBsonTypeException;
import org.smof.exception.SmofException;
import org.smof.field.MasterField;
import org.smof.field.SmofField;
import org.smof.index.InternalIndex;

@SuppressWarnings("javadoc")
public class SmofParser {

	private static void handleError(Throwable cause) {
		throw new SmofException(cause);
	}

	private final SmofTypeContext context;
	private final SmofParserPool parsers;
	private final LazyLoader lazyLoader;
	private final SerializationContext serContext;

	public SmofParser(SmofDispatcher dispatcher) {
		this.context = new SmofTypeContext();
		parsers = SmofParserPool.create(this, dispatcher);
		lazyLoader = LazyLoader.create(dispatcher);
		serContext = SerializationContext.create();
	}

	SmofTypeContext getContext() {
		return context;
	}
	
	SerializationContext getSerializationContext() {
		return serContext;
	}
	
	public <T> TypeStructure<T> getTypeStructure(Class<T> type) {
		return getContext().getTypeStructure(type, parsers);
	}
	
	<T extends Element> T createLazyInstance(Class<T> type, ObjectId id) {
		return lazyLoader.createLazyInstance(type, id);
	}

	public void registerType(Class<?> type) {
		context.put(type, parsers);
	}

	public void registerType(Class<?> type, Object factory){
		context.putWithFactory(type, factory, parsers);
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

	public BsonValue toBson(Object value, SmofField field) {
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

	SmofParserPool getParsers() {
		return parsers;
	}

	public <T extends Element> Set<InternalIndex> getIndexes(Class<T> elClass) {
		return context.getIndexes(elClass);
	}

	public void reset() {
		serContext.clear();
	}

	public SmofField getField(Class<?> type, String fieldName) {
		return getTypeStructure(type).getAllFields().get(fieldName);
	}
}
