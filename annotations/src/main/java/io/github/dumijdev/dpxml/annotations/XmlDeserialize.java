package io.github.dumijdev.dpxml.annotations;

import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface XmlDeserialize {
  Class<? extends XmlDeserializer<?>> using();
}
