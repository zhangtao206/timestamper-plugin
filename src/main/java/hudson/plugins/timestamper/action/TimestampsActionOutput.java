/*
 * The MIT License
 * 
 * Copyright (c) 2016 Steven G. Brown
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.timestamper.action;

import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Generate a page of time-stamps on behalf of {@link TimestampsAction}.
 * <p>
 * Each line contains the elapsed time in seconds since the start of the build
 * for the equivalent line in the console log.
 * <p>
 * By default, the elapsed time will include three places after the decimal
 * point. The number of places after the decimal point can be configured by the
 * "precision" query parameter, which accepts a number of decimal places or the
 * values "seconds" or "milliseconds".
 * 
 * @author Steven G. Brown
 */
public class TimestampsActionOutput {

  private static final Logger LOGGER = Logger
      .getLogger(TimestampsActionOutput.class.getName());

  private int precision;

  public TimestampsActionOutput() {
    setQuery(null);
  }

  /**
   * Set the query string.
   * 
   * @param query
   *          the query string
   */
  public void setQuery(String query) {
    precision = getPrecision(query);
  }

  private int getPrecision(String query) {
    String precision = getParameterValue(query, "precision");
    if ("seconds".equalsIgnoreCase(precision)) {
      return 0;
    }
    if ("milliseconds".equalsIgnoreCase(precision)) {
      return 3;
    }
    if ("microseconds".equalsIgnoreCase(precision)) {
      return 6;
    }
    if ("nanoseconds".equalsIgnoreCase(precision)) {
      return 9;
    }
    if (!Strings.isNullOrEmpty(precision)) {
      try {
        int intPrecision = Integer.parseInt(precision);
        if (intPrecision < 0) {
          logUnrecognisedPrecision(precision);
        } else {
          return intPrecision;
        }
      } catch (NumberFormatException ex) {
        logUnrecognisedPrecision(precision);
      }
    }
    // Default precision.
    return 3;
  }

  private String getParameterValue(String query, String parameterName) {
    if (query == null) {
      return null;
    }
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      String[] nameAndValue = pair.split("=", 2);
      if (nameAndValue.length == 2) {
        String key = urlDecode(nameAndValue[0]);
        String value = urlDecode(nameAndValue[1]);
        if (parameterName.equals(key)) {
          return value;
        }
      }
    }
    return null;
  }

  private String urlDecode(String string) {
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private void logUnrecognisedPrecision(String precision) {
    LOGGER.log(Level.WARNING, "Unrecognised precision: " + precision);
  }

  /**
   * Generate the next line in the page of time-stamps.
   * 
   * @param timestampsReader
   * @return the next line
   * @throws IOException
   */
  public Optional<String> nextLine(TimestampsReader timestampsReader)
      throws IOException {
    Optional<Timestamp> timestamp = timestampsReader.read();
    if (!timestamp.isPresent()) {
      return Optional.absent();
    }
    return Optional.of(formatTimestamp(timestamp.get(), precision));
  }

  private String formatTimestamp(Timestamp timestamp, int precision) {
    long seconds = timestamp.elapsedMillis / 1000;
    if (precision == 0) {
      return String.valueOf(seconds);
    }
    long millis = timestamp.elapsedMillis % 1000;
    String fractional = String.format("%03d", millis);
    if (precision <= 3) {
      fractional = fractional.substring(0, precision);
    } else {
      fractional += Strings.repeat("0", precision - 3);
    }
    return String.valueOf(seconds) + "." + fractional;
  }
}