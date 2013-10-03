package com.codexica.s3crate.filetree

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait FileTreeListener {

  /**
   * Called whenever a file path:
   * - is new
   * - was deleted
   * - possibly changed (content or metadata)
   *
   * Should not block while processing. If it's going to take a while, handle
   * your own threading and blocking.
   *
   * @param event The new event that just happened
   */
  def onNewFilePathEvent(event: FilePathEvent)
}
