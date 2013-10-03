package com.codexica.s3crate.filetree

/**
 * Listen to all events from a FileTree.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait ListenableFileTree {

  /**
   * @param listener The object to notify of new events
   * @return The generator that will cause each of those events
   */
  def listen(listener: FileTreeListener): PathGenerator
}
