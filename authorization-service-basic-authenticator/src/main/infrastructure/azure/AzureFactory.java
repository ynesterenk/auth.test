package com.github.vitalibo.authorization.basic.infrastructure.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.cognitiveservices.models.CognitiveServicesAccount;
import com.azure.resourcemanager.cognitiveservices.models.CognitiveServicesManager;
lombok.Getter;
lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AzureFactory {

    @Getter(lazy = true)
    private final CognitiveServicesManager cognitiveServicesManager =
        CognitiveServicesManager.authenticate(new DefaultAzureCredentialBuilder().build(), "<subscription-id>");

    @Getter(lazy = true)
    private final CognitiveServicesAccount cognitiveServicesAccount =
        cognitiveServicesManager.cognitiveServicesAccounts().getByResourceGroup("<resource-group>", "<account-name>");

    AzureFactory() {
    }

}
