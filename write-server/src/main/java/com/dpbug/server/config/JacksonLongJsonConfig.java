package com.dpbug.server.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson 序列化配置：
 * <p>
 * 解决雪花 ID（Long）在前端 JavaScript 中超过安全整数（2^53-1）导致精度丢失的问题。
 * <p>
 * - 输出：当 Long 超过 JS 安全整数范围时，序列化为字符串；否则保持数字类型。
 * - 输入：支持前端以字符串或数字传入 Long。
 * * @author dpbug
 */
@Configuration
public class JacksonLongJsonConfig {

    /**
     * JavaScript 最大安全整数（2^53 - 1）。
     */
    private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringWhenOverflowCustomizer() {
        JsonSerializer<Long> serializer = new SafeLongSerializer();
        JsonDeserializer<Long> deserializer = new SafeLongDeserializer();

        return builder -> {
            builder.serializerByType(Long.class, serializer);
            builder.serializerByType(Long.TYPE, serializer);
            builder.deserializerByType(Long.class, deserializer);
            builder.deserializerByType(Long.TYPE, deserializer);
        };
    }

    /**
     * Long 序列化：超过 JS 安全整数范围时输出字符串，避免前端精度丢失。
     */
    private static final class SafeLongSerializer extends StdScalarSerializer<Long> {

        private SafeLongSerializer() {
            super(Long.class);
        }

        @Override
        public void serialize(Long value, com.fasterxml.jackson.core.JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            long longValue = value;
            if (longValue > JS_MAX_SAFE_INTEGER || longValue < -JS_MAX_SAFE_INTEGER) {
                gen.writeString(Long.toString(longValue));
            } else {
                gen.writeNumber(longValue);
            }
        }
    }

    /**
     * Long 反序列化：兼容字符串/数字两种形式。
     */
    private static final class SafeLongDeserializer extends StdScalarDeserializer<Long> {

        private SafeLongDeserializer() {
            super(Long.class);
        }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken token = p.currentToken();

            if (token == JsonToken.VALUE_NUMBER_INT) {
                return p.getLongValue();
            }

            if (token == JsonToken.VALUE_STRING) {
                String raw = p.getText();
                if (raw == null) return null;
                String text = raw.trim();
                if (text.isEmpty()) return null;
                return Long.parseLong(text);
            }

            if (token == JsonToken.VALUE_NULL) {
                return null;
            }

            return (Long) ctxt.handleUnexpectedToken(Long.class, p);
        }
    }
}

