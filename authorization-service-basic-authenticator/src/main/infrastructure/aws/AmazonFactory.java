package com.github.vitalibo.authorization.basic.infrastructure.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AzureFactory {

    private final CognitiveServicesManager cognitiveServicesManager;

    @Getter(lazy = true)
    private final CognitiveServicesManager azureCognitiveServicesManager =
        CognitiveServicesManager.configure()
            .withHttpClient(new NettyAsyncHttpClientBuilder().build())
            .authenticate(new DefaultAzureCredentialBuilder().build(), new AzureProfile(AzureEnvironment.AZURE));

    AzureFactory() {
        this.cognitiveServicesManager = CognitiveServicesManager.configure()
            .withHttpClient(new NettyAsyncHttpClientBuilder().build())
            .authenticate(new DefaultAzureCredentialBuilder().build(), new AzureProfile(AzureEnvironment.AZURE));
    }

}
