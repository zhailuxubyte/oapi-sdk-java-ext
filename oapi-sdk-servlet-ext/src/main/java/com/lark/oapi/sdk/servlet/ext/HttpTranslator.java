package com.lark.oapi.sdk.servlet.ext;

import com.lark.oapi.core.request.EventReq;
import com.lark.oapi.core.response.EventResp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpTranslator {

  private String normalizeKey(String name) {
    return name != null ? name.toLowerCase() : null;
  }

  private Map<String, List<String>> toHeaderMap(HttpServletRequest req) {
    Map<String, List<String>> headers = new HashMap<>();
    Enumeration<String> names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      List<String> values = Collections.list(req.getHeaders(name));
      headers.put(normalizeKey(name), values);
    }
    return headers;
  }

  public EventReq translate(HttpServletRequest request) throws IOException {
    String bodyStr = request.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));
    EventReq req = new EventReq();
    req.setHeaders(toHeaderMap(request));
    req.setBody(bodyStr.getBytes(StandardCharsets.UTF_8));
    req.setHttpPath(request.getRequestURI());
    return req;
  }

  public void write(HttpServletResponse response, EventResp eventResp) throws IOException {
    response.setStatus(eventResp.getStatusCode());
    eventResp.getHeaders().entrySet().stream().forEach(keyValues -> {
      String key = keyValues.getKey();
      List<String> values = keyValues.getValue();
      values.stream().forEach(v -> response.addHeader(key, v));
    });
    if (eventResp.getBody() != null) {
      response.getWriter().write(new String(eventResp.getBody()));
    }
  }
}
