s3crate
=======

A secure, versioned (write-only), durable, efficient file synchronization system to uses S3 for storage.

- Secure: All file data and all meta-data is securely encrypted. Note that some information is revealed, namely the
approximate times and amounts of data that were modified, but this is deemed acceptable.

- Versioned: No data may ever be modified after being written. Instead, new files (or deltas on the old file) are
created and uploaded. Note that this trades off storage space (uses more) for safety (nothing can ever be lost except
by the failure in the underlying storage system)

- Durable: The chance of losing data from failures in the underlying storage mechanism should be as low as reasonably
possible. Ideally this will support duplicating the data to multiple locations and configurable tradeoffs for cost vs
data safety. Additionally, the system MUST continue to function even in the event of data loss--it should affect at
most the files whose blocks were lost.

- Efficient: Via a combination of smart deltas and compression, the system should be usable even on home internet
connections from normal computers.

Pronounced "see-crate" (it's meant to be a play on "secret", as well as "a place to dump all of your things in S3")

Motivation
==========

I could not find any existing software that fulfilled all 4 of the above attributes. There are some other
programs that are pretty close though:

- Dropbox: versioned, durable, efficient, but insecure. If you add EncFS on top of it, it becomes versioned, durable,
secure, but inefficient, because each tiny change in an encrypted file forces a synchronization of the entire file.
- S3ql: secure, durable (mostly? I was unclear about what happened when a file was lost in S3), efficient, but no
support for versioning.

Design
======

There are two ways of thinking about sets of files:
1. Point in time: This corresponds to your local file system. See ReadableFileTree, WritableFileTree, and
ListenableFileTree.
2. Changes over time: This corresponds to the FileTreeHistory class. It contains ever version of every file ever stored
in the FileTreeHistory, as well as methods for exploring the history, getting the latest versions, etc

The basic synchronization process (of local files in a LinuxFileTree to a remote S3FileHistory) works like this:

1. The local file tree continually generates a set of paths to be inspected

2. The TaskMaster actor (single instance) tracks which file change events have been dealt with, and responds to requests
for more work by sending a change event to handle

3. The Synchronizer actor is just a proxy between the workers and TaskMaster. It filters messages (if you want to
exclude certain files, for example) and forwards other requests on to the SynchronizationWorkers (there is a pool of these)

4. Each worker works on one synchronization task at a time. When a new task is started, it inspects the path locally and
remotely to determine if anything needs to be synchronized. In the case of deletes, meta data changes, renames, etc, just new
metadata is sent and no new file data needs to be uploaded. In the case of new files or updates to existing files, a
new blob is first uploaded, and then metadata linking to that blob is uploaded.

6. When blobs (file contents) are uploaded, each is encrypted with a unique symmetric key, which is stored, encrypted,
in the meta data object associated with that blob.

7. Blobs are zipped BEFORE being encrypted for efficiency.

8. Uploaded meta data is encrypted as well, using a separate set of keys. This means that you can make it impossible for
 the writing process to read the contents of the file after it is written (but you can read the contents from another
 device that does contain the private key, for example). Note that metadata must always be accessible by the writing process.

9. Future expansions include support for snapshotting filesystems such that deltas can be easily generated, zipped, encrypted
and uploaded, and then a list of blobs will be contained in the meta data object (and all must be downloaded to get the most recent data).
Additionally, it would be nice to be able to actually download your files, or see the status of the upload process.

NOTE: not everything described above works yet, but that's the goal :)

