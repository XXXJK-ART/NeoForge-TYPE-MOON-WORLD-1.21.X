package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import java.lang.reflect.InvocationTargetException;

final class ClientScreenAccess {
    private static final String CLIENT_HELPER_CLASS = "com.example.typemoonaddon.client.ClientPayloadScreens";

    private ClientScreenAccess() {
    }

    static void openDevourerSelection(OpenDevourerSelectionPayload payload) {
        invoke("openDevourerSelection", OpenDevourerSelectionPayload.class, payload);
    }

    static void openShadowTransfer(OpenShadowTransferPayload payload) {
        invoke("openShadowTransfer", OpenShadowTransferPayload.class, payload);
    }

    private static void invoke(String methodName, Class<?> payloadType, Object payload) {
        try {
            Class<?> helper = Class.forName(CLIENT_HELPER_CLASS);
            helper.getMethod(methodName, payloadType).invoke(null, payload);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            TypeMoonAddon.LOGGER.warn("Could not open client screen for {}", methodName, exception);
        } catch (LinkageError error) {
            TypeMoonAddon.LOGGER.warn("Client screen helper is unavailable for {}", methodName, error);
        }
    }
}
