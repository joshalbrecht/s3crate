package com.codexica.s3crate.filetree.history.synchronization

import com.codexica.s3crate.filetree.FilePath
import com.codexica.s3crate.filetree.history.FilePathState

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class PathTask(path: FilePath, source: FilePathState, dest: FilePathState)
