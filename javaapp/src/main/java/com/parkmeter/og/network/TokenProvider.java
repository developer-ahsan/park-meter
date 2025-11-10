package com.parkmeter.og.network;

import com.parkmeter.og.StripeTerminalApplication;
import com.parkmeter.og.model.AppState;
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;

/**
 * A simple implementation of the [ConnectionTokenProvider] interface. We just request a
 * new token from our backend simulator and forward any exceptions along to the SDK.
 */
public class TokenProvider implements ConnectionTokenProvider {

    @Override
    public void fetchConnectionToken(ConnectionTokenCallback callback) {
        try {
            // Get dynamic organization ID from app state
            AppState appState = StripeTerminalApplication.getInstance().getAppState();
            String organizationId = appState.getSelectedOrganizationId();
            
            if (organizationId == null || organizationId.isEmpty()) {
                // Throw exception if no organization ID is selected
                callback.onFailure(new ConnectionTokenException("No organization ID selected"));
                return;
            }
            
            final String token = ApiClient.createConnectionToken(organizationId);
            callback.onSuccess(token);
        } catch (ConnectionTokenException e) {
            callback.onFailure(e);
        }
    }
}
