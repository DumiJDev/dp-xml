package io.github.dumijdev.dpxml.annotations;

import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface XmlSerialize {
  Class<? extends XmlSerializer<?>> using();
}
