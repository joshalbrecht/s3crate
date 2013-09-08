package com.codexica.s3crate.common.models

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait SynchronizationDirection
case class Upload() extends SynchronizationDirection
case class Download() extends SynchronizationDirection
