package graphicUI;

import entity.NguoiDung;

/**
 * SessionContext keeps track of the currently authenticated user so the UI can
 * enforce role-based rules without re-prompting for a password.
 */
public final class SessionContext {
    private static NguoiDung currentUser;

    private SessionContext() {}

    public static void setCurrentUser(NguoiDung user) {
        currentUser = user;
    }

    public static NguoiDung getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && "quanLy".equalsIgnoreCase(currentUser.getVaiTro());
    }
}
