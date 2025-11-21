package org.vidyaastra.ui.exception;

public class VidyaastraException extends Exception
{
   private static final long serialVersionUID = 1L;

   public VidyaastraException(String message)
   {
      super(message);
   }

   public VidyaastraException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public VidyaastraException(Throwable cause)
   {
      super(cause);
   }
}
