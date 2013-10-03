package com.codexica.s3crate.filetree

//TODO: sort events in a priority queue based on the time of modification
//TODO: make a new method on the FileSystem for watching for changes
/**
 * A stream of events. Has no end. Will block if there are no more events.
 * Returns events in order from highest priority to lowest priority, which is based on the modification date
 * of the file (oldest should be synchronized first
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class PathGenerator(allPaths: Set[FilePathEvent],
                    files: ListenableFileTree,
                    listener: FileTreeListener) {

  def start() {
    allPaths.foreach(event => {
      listener.onNewFilePathEvent(event)
    })
  }

  def stop() {

  }
}
