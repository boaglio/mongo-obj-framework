package org.smof.bson.codecs.string;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.smof.bson.codecs.SmofCodecProvider;

import com.google.common.base.Objects;

@SuppressWarnings("javadoc")
public class SmofStringCodecProvider implements SmofCodecProvider {
	
	private final Codec<String> stringCodec;
	
	public SmofStringCodecProvider() {
		stringCodec = new StringCodec();
	}

	@SuppressWarnings({ "unchecked", "cast", "rawtypes" })
	@Override
	public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
		if(String.class.equals(clazz)) {
			return (Codec<T>) stringCodec;
		}
		if(Enum.class.isAssignableFrom(clazz)) {
			return (Codec<T>) new EnumCodec(clazz);
		}
		return null;
	}

	@Override
	public boolean contains(Class<?> clazz) {
		return Objects.equal(clazz, String.class);
	}

}
