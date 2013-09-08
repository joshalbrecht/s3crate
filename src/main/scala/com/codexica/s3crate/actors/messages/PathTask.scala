package com.codexica.s3crate.actors.messages

import com.codexica.s3crate.common.models.{FilePathState, FilePath}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class PathTask(path: FilePath, source: FilePathState, dest: FilePathState)
