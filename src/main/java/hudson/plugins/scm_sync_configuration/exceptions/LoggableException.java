package hudson.plugins.scm_sync_configuration.exceptions;

/**
 * @author fcamblor
 * Exception which will be easily loggable, by providing both class and method called, causing the exception
 */
public class LoggableException extends RuntimeException {
    Class clazz;
    String methodName;

    public LoggableException(String message, Class clazz, String methodName, Throwable cause) {
        super(message, cause);
        this.clazz = clazz;
        this.methodName = methodName;
    }

    public LoggableException(String message, Class clazz, String methodName) {
        super(message);
        this.clazz = clazz;
        this.methodName = methodName;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getMethodName() {
        return methodName;
    }
}
