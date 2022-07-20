package com.larksuite.oapi.sdk.apache.httpclient;

import com.lark.oapi.core.httpclient.IHttpTransport;
import com.lark.oapi.core.request.FormData;
import com.lark.oapi.core.request.FormDataFile;
import com.lark.oapi.core.request.RawRequest;
import com.lark.oapi.core.response.RawResponse;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.core.utils.Strings;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class ApacheHttpClientTransport implements IHttpTransport {

  private CloseableHttpClient httpclient;

  private ApacheHttpClientTransport(Builder builder) {
    httpclient = builder.httpclient;
  }

  public void Close() throws IOException {
    httpclient.close();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public RawResponse execute(RawRequest rawRequest) throws Exception {
    final String httpMethod = rawRequest.getHttpMethod();
    // 创建请求对象request
    HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
      @Override
      public String getMethod() {
        return httpMethod;
      }
    };

    // 设置url，headers
    request.setURI(URI.create(rawRequest.getReqUrl()));
    for (Map.Entry<String, List<String>> entry : rawRequest.getHeaders().entrySet()) {
      String key = entry.getKey();
      for (String value : entry.getValue()) {
        request.addHeader(key, value);
      }
    }

    // 设置body
    if (rawRequest.getBody() != null) {
      Object body = rawRequest.getBody();
      if (body instanceof FormData) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.setContentType(ContentType.create(ContentType.MULTIPART_FORM_DATA.getMimeType()));
        for (FormDataFile file : ((FormData) rawRequest.getBody()).getFiles()) {
          builder.addBinaryBody(file.getFieldName(), file.getFile(),
              ContentType.APPLICATION_OCTET_STREAM,
              Strings.isEmpty(file.getFileName()) ? "unknown" : file.getFileName());
        }
        for (Map.Entry<String, Object> entry : ((FormData) rawRequest.getBody()).getParams()
            .entrySet()) {
          builder.addTextBody(entry.getKey(), (String) entry.getValue());
        }
        request.setEntity(builder.build());
      } else {
        StringEntity entity = new StringEntity(Jsons.LONG_TO_STR.toJson(rawRequest.getBody()));
        request.setEntity(entity);
      }
    }

    // 发起调用
    CloseableHttpResponse response = httpclient.execute(request);

    // 转换结果为通用结果
    byte[] result = EntityUtils.toByteArray(response.getEntity());
    RawResponse rawResponse = new RawResponse();
    rawResponse.setStatusCode(response.getStatusLine().getStatusCode());
    rawResponse.setBody(result);
    rawResponse.setContentType(rawResponse.getContentType());
    Map<String, List<String>> headers = new HashMap<>();
    for (Header header : response.getAllHeaders()) {
      if (headers.containsKey(header.getName())) {
        headers.get(header.getName()).add(header.getValue());
      } else {
        List<String> values = new ArrayList<>();
        values.add(header.getValue());
        headers.put(header.getName(), values);
      }
    }
    rawResponse.setHeaders(headers);
    return rawResponse;
  }

  public static final class Builder {

    private CloseableHttpClient httpclient;

    private Builder() {
    }

    public Builder httpclient(CloseableHttpClient httpclient) {
      this.httpclient = httpclient;
      return this;
    }

    public ApacheHttpClientTransport build() {
      return new ApacheHttpClientTransport(this);
    }
  }
}
