package graphicUI;

/**
 * AppConfig holds simple application-wide configuration such as the system password.
 */
public class AppConfig {
    // Default system password (change via SystemPanel)
    private static String systemPassword = "admin123";

    public static String getSystemPassword() {
        return systemPassword;
    }

    public static void setSystemPassword(String newPassword) {
        if (newPassword != null && newPassword.length() > 0) {
            systemPassword = newPassword;
        }
    }
}
