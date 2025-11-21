package org.vidyaastra.ui.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtils
{
   private static final Logger logger = LogManager.getLogger(LogUtils.class);

   public static void logInfo(String message)
   {
      logger.info(message);
   }

   public static void logError(String message, Throwable throwable)
   {
      logger.error(message, throwable);
   }

   public static void logWarning(String message)
   {
      logger.warn(message);
   }

   public static void logDebug(String message)
   {
      logger.debug(message);
   }
}
