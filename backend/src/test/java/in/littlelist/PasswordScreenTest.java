package in.littlelist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordScreenTest {
 @Test void breachRangeDistinguishesPaddingAndOtherHashes() {
  assertTrue(PasswordScreen.compromised("ABC123", "OTHER:89\r\nabc123:12\r\n"));
  assertFalse(PasswordScreen.compromised("ABC123", "ABC123:0\r\nOTHER:89"));
  assertFalse(PasswordScreen.compromised("ABC123", "ABC123:000\r\nABC123:invalid\r\nOTHER:3"));
 }
}
