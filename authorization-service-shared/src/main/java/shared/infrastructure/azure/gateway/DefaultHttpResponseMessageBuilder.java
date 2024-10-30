package shared.infrastructure.azure.gateway;


import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatusType;

public class DefaultHttpResponseMessageBuilder implements HttpResponseMessage.Builder {

    private Object body;

    private final HttpStatusType status;

    public DefaultHttpResponseMessageBuilder(HttpStatusType status) {
        this.status = status;
    }

    @Override
    public HttpResponseMessage.Builder status(HttpStatusType status) {
        return new DefaultHttpResponseMessageBuilder(status);
    }



    @Override
    public HttpResponseMessage.Builder header(String s, String s1) {
        return null;
    }

    @Override
    public HttpResponseMessage.Builder body(Object body) {
        this.body = body;
        return this;
    }

    @Override
    public HttpResponseMessage build() {
        return new DefaultHttpResponseMessage(status, body);
    }

    // Custom HttpResponseMessage implementation for the builder
    private static class DefaultHttpResponseMessage implements HttpResponseMessage {
        private final HttpStatusType status;
        private final Object body;

        public DefaultHttpResponseMessage(HttpStatusType status, Object body) {
            this.status = status;
            this.body = body;
        }

        @Override
        public HttpStatusType getStatus() {
            return status;
        }

        @Override
        public String getHeader(String s) {
            return "";
        }

        @Override
        public Object getBody() {
            return body;
        }


    }
}
