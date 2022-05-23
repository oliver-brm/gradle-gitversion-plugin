package org.unbrokendome.gradle.plugins.gitversion;

import org.spockframework.runtime.extension.ExtensionAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a Git repository for tests and deletes it afterwards.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@ExtensionAnnotation(TestRepositoryExtension.class)
public @interface TestRepository {
}
