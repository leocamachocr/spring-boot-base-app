package dev.leocamacho.demo.session;


import org.springframework.security.core.context.SecurityContextHolder;

public class SessionContextHolder {

    public static Session getSession() {
        if (SecurityContextHolder.getContext() == null) {
            setSession(Session.newBuilder().buildAnonymous());
        }
        return (Session) SecurityContextHolder.getContext().getAuthentication();
    }

    public static void setSession(Session session) {
        SecurityContextHolder.getContext().setAuthentication(session);
    }


}
