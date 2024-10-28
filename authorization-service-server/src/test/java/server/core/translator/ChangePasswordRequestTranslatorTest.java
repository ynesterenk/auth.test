package server.core.translator;

import authorization.server.core.model.ChangePasswordRequest;
import authorization.server.core.translator.ChangePasswordRequestTranslator;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import shared.infrastructure.azure.gateway.proxy.HttpHeaders;
import com.microsoft.azure.functions.HttpRequestMessage;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static server.TestHelper.makeHttpRequestTranslator;

public class ChangePasswordRequestTranslatorTest {

    @Test
    public void testTranslate()  throws UnsupportedEncodingException {
        HttpRequestTranslator request = makeHttpRequestTranslator(
                "username=admin&previous_password=Welcome2017!&proposed_password=Aq1Sw2De3");

        ChangePasswordRequest actual = ChangePasswordRequestTranslator.from(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getUsername(), "admin");
        Assert.assertEquals(actual.getPreviousPassword(), "Welcome2017!");
        Assert.assertEquals(actual.getProposedPassword(), "Aq1Sw2De3");
    }

    @Test
    public void testTranslateEmpty()  throws UnsupportedEncodingException {
        HttpRequestTranslator request = makeHttpRequestTranslator("");

        ChangePasswordRequest actual = ChangePasswordRequestTranslator.from(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, new ChangePasswordRequest());
    }


}
