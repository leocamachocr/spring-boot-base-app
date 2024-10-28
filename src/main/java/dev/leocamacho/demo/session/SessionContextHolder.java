package dev.leocamacho.demo.session;


import org.springframework.security.core.context.SecurityContextHolder;

public class SessionContextHolder {

    //generate thread variable
   // private static final ThreadLocal<Session> contextHolder = new ThreadLocal<>();

    public static Session getSession() {
        return (Session) SecurityContextHolder.getContext().getAuthentication();
    }

    public static void setSession(Session session) {

   //     if (contextHolder.get() != null) {
         //   throw new IllegalStateException("Session already set");
      //  }
        SecurityContextHolder.getContext().setAuthentication(session);
     //   contextHolder.set(session);
    }


}
