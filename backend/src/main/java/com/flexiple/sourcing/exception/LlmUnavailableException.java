package com.flexiple.sourcing.exception;

/** Timeout, connection failure or provider 5xx — already retried before surfacing. */
public class LlmUnavailableException extends LlmException {

    public LlmUnavailableException(String technicalMessage, Throwable cause) {
        super(technicalMessage,
                "The model did not respond in time. Nothing was lost — try that step again.",
                cause);
    }

    private LlmUnavailableException(String technicalMessage, String userFacingMessage, Throwable cause) {
        super(technicalMessage, userFacingMessage, cause);
    }

    /**
     * The provider could not be reached at all — DNS failed, or there is no route.
     *
     * <p>Worth its own message: "did not respond in time" sends someone looking at their API
     * key or the provider's status page, when the actual problem is usually their own network
     * — a dropped connection, a VPN, or a container that has lost DNS.
     */
    public static LlmUnavailableException unreachable(String technicalMessage, Throwable cause) {
        return new LlmUnavailableException(technicalMessage,
                "Could not reach the model provider. Check the network connection, then try again.",
                cause);
    }

    /** True when the cause is a name-resolution or connection failure rather than a slow reply. */
    public static boolean isNetworkFailure(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.nio.channels.UnresolvedAddressException
                    || cause instanceof java.net.UnknownHostException
                    || cause instanceof java.net.ConnectException
                    || cause instanceof java.net.NoRouteToHostException) {
                return true;
            }
        }
        return false;
    }
}
