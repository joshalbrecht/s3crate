package com.codexica.s3crate.actors.messages

import com.codexica.s3crate.filesystem.{FilePath, FileSnapshot}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class PathTask(path: FilePath, source: Option[FileSnapshot], dest: Option[FileSnapshot])
