package com.codexica.s3crate.filesystem

import java.io.InputStream

//TODO: change both accessors to be functions that load the data on demand, NOT during object creation
//This will be more efficient any time where we don't end up using it, and localizes error handling to just the usages,
//where it must be handled, and NOT creation, where we can ignore it.
//A consequence of this is that ReadableFiles may be created for any path/nonsense that doesnt necessarily exist
/**
 *
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class ReadableFile(val data: () => InputStream, val length: Long)
