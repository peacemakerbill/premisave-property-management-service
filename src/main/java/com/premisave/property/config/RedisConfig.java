package com.premisave.property.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
public class RedisConfig {

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());

        // GenericJackson2JsonRedisSerializer was deprecated (for removal) in Spring Data Redis
        // 4.0, in favor of this Jackson 3-based serializer. Unlike its predecessor, it does NOT
        // enable default typing (embedding "@class" in the JSON) by default, which this
        // RedisTemplate<String, Object> relies on to deserialize back to the original type —
        // so default typing is re-enabled explicitly here, scoped to this project's own classes
        // (plus java.util collections) rather than any class on the classpath.
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.premisave.property.")
                .allowIfSubType("java.util.")
                .build();

        template.setValueSerializer(GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build());

        return template;
    }
}