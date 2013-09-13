package com.codexica.common

import java.net.InetAddress
import scala.util.control.NonFatal
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object MachineUtils {

  /**
   * Will do its absolute best to return some name that identifies this computer.
   *
   * @return An identifying name for this computer, or "unknown" if none could be found
   */
  @Loggable(value = Loggable.TRACE, limit = 1, unit = TimeUnit.SECONDS, prepend = true)
  def getHostName: String = {
    try {
      if (System.getProperty("os.name").startsWith("Windows")) {
        // Windows will always set the 'COMPUTERNAME' variable
        System.getenv("COMPUTERNAME")
      } else {
        // If it is not Windows then it is most likely a Unix-like operating system
        // such as Solaris, AIX, HP-UX, Linux or MacOS.

        // Most modern shells (such as Bash or derivatives) sets the
        // HOSTNAME variable so lets try that first.
        val hostname = System.getenv("HOSTNAME")
        if (hostname != null) {
          hostname
        } else {

          // If the above returns null *and* the OS is Unix-like
          // then you can try an exec() and read the output from the
          // 'hostname' command which exist on all types of Unix/Linux.

          // If you are an OS other than Unix/Linux then you would have
          // to do something else. For example on OpenVMS you would find
          // it like this from the shell:  F$GETSYI("NODENAME")
          // which you would probably also have to find from within Java
          // via an exec() call.

          // If you are on zOS then who knows ??

          // etc, etc
          InetAddress.getLocalHost.getHostName
        }
      }
    } catch {
      case NonFatal(t) => "unknown"
    }
  }
}
