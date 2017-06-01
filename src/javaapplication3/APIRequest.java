/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication3;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.drive.Drive;

// ...

public class APIRequest {
  private static final String APPLICATION_NAME = "JavaApplication3";
  private static final JacksonFactory JSON_FACTORY =
      JacksonFactory.getDefaultInstance();
  private static final HttpTransport HTTP_TRANSPORT =
      new NetHttpTransport();

  // ...

  /**
   * Build a Drive service object.
   *
   * @param credentials OAuth 2.0 credentials.
   * @return Drive service object.
   */
  static Drive buildService(Credential credentials) {
    return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  // ...
}
/**
 *
 * @author tiago.lucas
 */
