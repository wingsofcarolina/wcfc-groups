package org.wingsofcarolina.groups.http;

public class APIException extends Exception {

  private static final long serialVersionUID = 2590503290636933153L;

  public APIException(String message) {
    super(message);
  }
}
