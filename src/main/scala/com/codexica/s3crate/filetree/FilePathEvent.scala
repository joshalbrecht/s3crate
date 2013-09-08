package com.codexica.s3crate.filetree

import org.joda.time.DateTime

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FilePathEvent(path: FilePath, lastModified: DateTime)
