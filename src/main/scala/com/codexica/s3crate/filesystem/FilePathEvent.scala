package com.codexica.s3crate.filesystem

import org.joda.time.DateTime

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FilePathEvent(path: FilePath, lastModified: DateTime)
