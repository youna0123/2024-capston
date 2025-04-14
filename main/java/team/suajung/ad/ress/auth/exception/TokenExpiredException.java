package team.suajung.ad.ress.auth.exception;

public class TokenExpiredException extends RuntimeException {
  private String userId;

  public TokenExpiredException(String message, String userId) {
    super(message);
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }
}
