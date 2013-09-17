package com.codexica.s3crate.filetree.history.snapshotstore.s3;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Distinguish which execution context should be used for S3 connections
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
@BindingAnnotation @Target({ FIELD, PARAMETER, METHOD }) @Retention(RUNTIME)
public @interface S3ExecutionContext {}
