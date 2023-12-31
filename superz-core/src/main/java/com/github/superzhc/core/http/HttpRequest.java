/*
 * Copyright (c) 2014 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.github.superzhc.core.http;

import com.github.superzhc.core.crypto.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.Proxy.Type.HTTP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A fluid interface for making HTTP requests using an underlying
 * {@link HttpURLConnection} (or sub-class).
 * <p>
 * Each instance supports making a single request and cannot be reused for
 * further requests.
 */
public class HttpRequest {

    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    /**
     * 'UTF-8' charset name
     */
    public static final String CHARSET_UTF8 = "UTF-8";

    /**
     * 'application/x-www-form-urlencoded' content type header value
     */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * 'application/json' content type header value
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 'gzip' encoding header value
     */
    public static final String ENCODING_GZIP = "gzip";

    /**
     * 'Accept' header name
     */
    public static final String HEADER_ACCEPT = "Accept";

    /**
     * 'Accept-Charset' header name
     */
    public static final String HEADER_ACCEPT_CHARSET = "Accept-Charset";

    /**
     * 'Accept-Encoding' header name
     */
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * 'Authorization' header name
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * 'Cache-Control' header name
     */
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /**
     * 'Content-Encoding' header name
     */
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

    /**
     * 'Content-Length' header name
     */
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * 'Content-Type' header name
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * 'Date' header name
     */
    public static final String HEADER_DATE = "Date";

    /**
     * 'ETag' header name
     */
    public static final String HEADER_ETAG = "ETag";

    /**
     * 'Expires' header name
     */
    public static final String HEADER_EXPIRES = "Expires";

    /**
     * 'If-None-Match' header name
     */
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    /**
     * 'Last-Modified' header name
     */
    public static final String HEADER_LAST_MODIFIED = "Last-Modified";

    /**
     * 'Location' header name
     */
    public static final String HEADER_LOCATION = "Location";

    /**
     * 'Proxy-Authorization' header name
     */
    public static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * 'Referer' header name
     */
    public static final String HEADER_REFERER = "Referer";

    /**
     * 'Server' header name
     */
    public static final String HEADER_SERVER = "Server";

    /**
     * 'User-Agent' header name
     */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /**
     * 'Cookie' header name
     */
    public static final String HEADER_COOKIE="Cookie";

    /**
     * 'DELETE' request method
     */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * 'GET' request method
     */
    public static final String METHOD_GET = "GET";

    /**
     * 'HEAD' request method
     */
    public static final String METHOD_HEAD = "HEAD";

    /**
     * 'OPTIONS' options method
     */
    public static final String METHOD_OPTIONS = "OPTIONS";

    /**
     * 'POST' request method
     */
    public static final String METHOD_POST = "POST";

    /**
     * 'PUT' request method
     */
    public static final String METHOD_PUT = "PUT";

    /**
     * 'TRACE' request method
     */
    public static final String METHOD_TRACE = "TRACE";

    /**
     * 'charset' header value parameter
     */
    public static final String PARAM_CHARSET = "charset";

    private static final String BOUNDARY = "00content0boundary00";

    private static final String CONTENT_TYPE_MULTIPART = "multipart/form-data; boundary="
            + BOUNDARY;

    private static final String CRLF = "\r\n";

    private static final String[] EMPTY_STRINGS = new String[0];

    private static SSLSocketFactory TRUSTED_FACTORY;

    private static HostnameVerifier TRUSTED_VERIFIER;

    private static String getValidCharset(final String charset) {
        if (charset != null && charset.length() > 0)
            return charset;
        else
            return CHARSET_UTF8;
    }

    private static SSLSocketFactory getTrustedFactory()
            throws HttpRequestException {
        if (TRUSTED_FACTORY == null) {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Intentionally left blank
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Intentionally left blank
                }
            }};
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, new SecureRandom());
                TRUSTED_FACTORY = context.getSocketFactory();
            } catch (GeneralSecurityException e) {
                IOException ioException = new IOException(
                        "Security exception configuring SSL context");
                ioException.initCause(e);
                throw new HttpRequestException(ioException);
            }
        }

        return TRUSTED_FACTORY;
    }

    private static HostnameVerifier getTrustedVerifier() {
        if (TRUSTED_VERIFIER == null)
            TRUSTED_VERIFIER = new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

        return TRUSTED_VERIFIER;
    }

    private static StringBuilder addPathSeparator(final String baseUrl,
                                                  final StringBuilder result) {
        // Add trailing slash if the base URL doesn't have any path segments.
        //
        // The following test is checking for the last slash not being part of
        // the protocol to host separator: '://'.
        if (baseUrl.indexOf(':') + 2 == baseUrl.lastIndexOf('/'))
            result.append('/');
        return result;
    }

    private static StringBuilder addParamPrefix(final String baseUrl,
                                                final StringBuilder result) {
        // Add '?' if missing and add '&' if params already exist in base url
        final int queryStart = baseUrl.indexOf('?');
        final int lastChar = result.length() - 1;
        if (queryStart == -1)
            result.append('?');
        else if (queryStart < lastChar && baseUrl.charAt(lastChar) != '&')
            result.append('&');
        return result;
    }

    private static StringBuilder addParam(final Object key, Object value,
                                          final StringBuilder result) {
        if (value != null && value.getClass().isArray())
            value = arrayToList(value);

        if (value instanceof Iterable<?>) {
            /**
             * 传递数组的几种方式：
             * 1. http://localhost:8080/users?roleIds=1&roleIds=2
             * 2. http://localhost:8080/users?roleIds=1,2【待验证】
             * 3. http://localhost:8080/users?roleIds[0]=1&roleIds[1]=2
             * 4. http://localhost:8080/users?roleIds[]=1&roleIds[]=2
             */
            Iterator<?> iterator = ((Iterable<?>) value).iterator();
            while (iterator.hasNext()) {
                result.append(key);
                // result.append("[]=");
                result.append("=");
                Object element = iterator.next();
                if (element != null)
                    result.append(element);
                if (iterator.hasNext())
                    result.append("&");
            }
        } else {
            result.append(key);
            result.append("=");
            if (value != null)
                result.append(value);
        }

        return result;
    }

    /**
     * Creates {@link HttpURLConnection HTTP connections} for
     * {@link URL urls}.
     */
    public interface ConnectionFactory {
        /**
         * Open an {@link HttpURLConnection} for the specified {@link URL}.
         *
         * @throws IOException
         */
        HttpURLConnection create(URL url) throws IOException;

        /**
         * Open an {@link HttpURLConnection} for the specified {@link URL}
         * and {@link Proxy}.
         *
         * @throws IOException
         */
        HttpURLConnection create(URL url, Proxy proxy) throws IOException;

        /**
         * A {@link ConnectionFactory} which uses the built-in
         * {@link URL#openConnection()}
         */
        ConnectionFactory DEFAULT = new ConnectionFactory() {
            public HttpURLConnection create(URL url) throws IOException {
                return (HttpURLConnection) url.openConnection();
            }

            public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
                return (HttpURLConnection) url.openConnection(proxy);
            }
        };
    }

    private static ConnectionFactory CONNECTION_FACTORY = ConnectionFactory.DEFAULT;

    /**
     * Specify the {@link ConnectionFactory} used to create new requests.
     */
    public static void setConnectionFactory(final ConnectionFactory connectionFactory) {
        if (connectionFactory == null)
            CONNECTION_FACTORY = ConnectionFactory.DEFAULT;
        else
            CONNECTION_FACTORY = connectionFactory;
    }

    /**
     * Callback interface for reporting upload progress for a request.
     */
    public interface UploadProgress {
        /**
         * Callback invoked as data is uploaded by the request.
         *
         * @param uploaded The number of bytes already uploaded
         * @param total    The total number of bytes that will be uploaded or -1 if
         *                 the length is unknown.
         */
        void onUpload(long uploaded, long total);

        UploadProgress DEFAULT = new UploadProgress() {
            public void onUpload(long uploaded, long total) {
            }
        };
    }

    /**
     * HTTP request exception whose cause is always an {@link IOException}
     */
    public static class HttpRequestException extends RuntimeException {

        private static final long serialVersionUID = -1170466989781746231L;

        /**
         * Create a new HttpRequestException with the given cause
         *
         * @param cause
         */
        public HttpRequestException(final IOException cause) {
            super(cause);
        }

        /**
         * Get {@link IOException} that triggered this request exception
         *
         * @return {@link IOException} cause
         */
        @Override
        public IOException getCause() {
            return (IOException) super.getCause();
        }
    }

    /**
     * Operation that handles executing a callback once complete and handling
     * nested exceptions
     *
     * @param <V>
     */
    protected static abstract class Operation<V> implements Callable<V> {

        /**
         * Run operation
         *
         * @return result
         * @throws HttpRequestException
         * @throws IOException
         */
        protected abstract V run() throws HttpRequestException, IOException;

        /**
         * Operation complete callback
         *
         * @throws IOException
         */
        protected abstract void done() throws IOException;

        public V call() throws HttpRequestException {
            boolean thrown = false;
            try {
                return run();
            } catch (HttpRequestException e) {
                thrown = true;
                throw e;
            } catch (IOException e) {
                thrown = true;
                throw new HttpRequestException(e);
            } finally {
                try {
                    done();
                } catch (IOException e) {
                    if (!thrown)
                        throw new HttpRequestException(e);
                }
            }
        }
    }

    /**
     * Class that ensures a {@link Closeable} gets closed with proper exception
     * handling.
     *
     * @param <V>
     */
    protected static abstract class CloseOperation<V> extends Operation<V> {

        private final Closeable closeable;

        private final boolean ignoreCloseExceptions;

        /**
         * Create closer for operation
         *
         * @param closeable
         * @param ignoreCloseExceptions
         */
        protected CloseOperation(final Closeable closeable,
                                 final boolean ignoreCloseExceptions) {
            this.closeable = closeable;
            this.ignoreCloseExceptions = ignoreCloseExceptions;
        }

        @Override
        protected void done() throws IOException {
            if (closeable instanceof Flushable)
                ((Flushable) closeable).flush();
            if (ignoreCloseExceptions)
                try {
                    closeable.close();
                } catch (IOException e) {
                    // Ignored
                }
            else
                closeable.close();
        }
    }

    /**
     * Class that and ensures a {@link Flushable} gets flushed with proper
     * exception handling.
     *
     * @param <V>
     */
    protected static abstract class FlushOperation<V> extends Operation<V> {

        private final Flushable flushable;

        /**
         * Create flush operation
         *
         * @param flushable
         */
        protected FlushOperation(final Flushable flushable) {
            this.flushable = flushable;
        }

        @Override
        protected void done() throws IOException {
            flushable.flush();
        }
    }

    /**
     * Request output stream
     */
    public static class RequestOutputStream extends BufferedOutputStream {

        private final CharsetEncoder encoder;

        /**
         * Create request output stream
         *
         * @param stream
         * @param charset
         * @param bufferSize
         */
        public RequestOutputStream(final OutputStream stream, final String charset,
                                   final int bufferSize) {
            super(stream, bufferSize);

            encoder = Charset.forName(getValidCharset(charset)).newEncoder();
        }

        /**
         * Write string to stream
         *
         * @param value
         * @return this stream
         * @throws IOException
         */
        public RequestOutputStream write(final String value) throws IOException {
            final ByteBuffer bytes = encoder.encode(CharBuffer.wrap(value));

            super.write(bytes.array(), 0, bytes.limit());

            return this;
        }
    }

    /**
     * Represents array of any type as list of objects so we can easily iterate over it
     *
     * @param array of elements
     * @return list with the same elements
     */
    private static List<Object> arrayToList(final Object array) {
        if (array instanceof Object[])
            return Arrays.asList((Object[]) array);

        List<Object> result = new ArrayList<Object>();
        // Arrays of the primitive types can't be cast to array of Object, so this:
        if (array instanceof int[])
            for (int value : (int[]) array) result.add(value);
        else if (array instanceof boolean[])
            for (boolean value : (boolean[]) array) result.add(value);
        else if (array instanceof long[])
            for (long value : (long[]) array) result.add(value);
        else if (array instanceof float[])
            for (float value : (float[]) array) result.add(value);
        else if (array instanceof double[])
            for (double value : (double[]) array) result.add(value);
        else if (array instanceof short[])
            for (short value : (short[]) array) result.add(value);
        else if (array instanceof byte[])
            for (byte value : (byte[]) array) result.add(value);
        else if (array instanceof char[])
            for (char value : (char[]) array) result.add(value);
        return result;
    }

    /**
     * Encode the given URL as an ASCII {@link String}
     * <p>
     * This method ensures the path and query segments of the URL are properly
     * encoded such as ' ' characters being encoded to '%20' or any UTF-8
     * characters that are non-ASCII. No encoding of URLs is done by default by
     * the {@link HttpRequest} constructors and so if URL encoding is needed this
     * method should be called before calling the {@link HttpRequest} constructor.
     *
     * @param url
     * @return encoded URL
     * @throws HttpRequestException
     */
    public static String encode(final CharSequence url)
            throws HttpRequestException {
        URL parsed;
        try {
            parsed = new URL(url.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }

        // 2023年4月13日 bug：用户的auth信息丢失，已修复
        String host = parsed.getHost();
        int port = parsed.getPort();
//        if (port != -1)
//            host = host + ':' + Integer.toString(port);

        try {
//            String encoded = new URI(parsed.getProtocol(), host, parsed.getPath(),
//                    parsed.getQuery(), null).toASCIIString();
            String encoded = new URI(parsed.getProtocol(), parsed.getUserInfo(), host, port, parsed.getPath(),
                    parsed.getQuery(), null).toASCIIString();
            int paramsStart = encoded.indexOf('?');
            if (paramsStart > 0 && paramsStart + 1 < encoded.length())
                encoded = encoded.substring(0, paramsStart + 1)
                        // 参数部分自定义转义部分
                        + encoded.substring(paramsStart + 1)
                        .replace("+", "%2B")
                        .replace(":", "%3A")
                        .replace(",", "%2C")
                        .replace("(", "%28")
                        .replace(")", "%29")
                        ;
            return encoded;
        } catch (URISyntaxException e) {
            IOException io = new IOException("Parsing URI failed");
            io.initCause(e);
            throw new HttpRequestException(io);
        }
    }

    /**
     * Append given map as query parameters to the base URL
     * <p>
     * Each map entry's key will be a parameter name and the value's
     * {@link Object#toString()} will be the parameter value.
     *
     * @param url
     * @param params
     * @return URL with appended query params
     */
    public static String append(final CharSequence url, final Map<?, ?> params) {
        final String baseUrl = url.toString();
        if (params == null || params.isEmpty())
            return baseUrl;

        final StringBuilder result = new StringBuilder(baseUrl);

        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        Entry<?, ?> entry;
        Iterator<?> iterator = params.entrySet().iterator();
        entry = (Entry<?, ?>) iterator.next();
        addParam(entry.getKey().toString(), entry.getValue(), result);

        while (iterator.hasNext()) {
            result.append('&');
            entry = (Entry<?, ?>) iterator.next();
            addParam(entry.getKey().toString(), entry.getValue(), result);
        }

        return result.toString();
    }

    /**
     * Append given name/value pairs as query parameters to the base URL
     * <p>
     * The params argument is interpreted as a sequence of name/value pairs so the
     * given number of params must be divisible by 2.
     *
     * @param url
     * @param params name/value pairs
     * @return URL with appended query params
     */
    public static String append(final CharSequence url, final Object... params) {
        final String baseUrl = url.toString();
        if (params == null || params.length == 0)
            return baseUrl;

        if (params.length % 2 != 0)
            throw new IllegalArgumentException(
                    "Must specify an even number of parameter names/values");

        final StringBuilder result = new StringBuilder(baseUrl);

        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        addParam(params[0], params[1], result);

        for (int i = 2; i < params.length; i += 2) {
            result.append('&');
            addParam(params[i], params[i + 1], result);
        }

        return result.toString();
    }

    /**
     * Start a 'GET' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest get(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_GET);
    }

    /**
     * Start a 'GET' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest get(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_GET);
    }

    public static HttpRequest get(final CharSequence baseUrl,
                                  final Map<?, ?> params) {
        return get(baseUrl, params, true);
    }

    /**
     * Start a 'GET' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  The query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static HttpRequest get(final CharSequence baseUrl,
                                  final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return get(encode ? encode(url) : url);
    }

    /**
     * Start a 'GET' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the
     *                baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static HttpRequest get(final CharSequence baseUrl,
                                  final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return get(encode ? encode(url) : url);
    }

    /**
     * Start a 'POST' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest post(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_POST);
    }

    /**
     * Start a 'POST' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest post(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_POST);
    }

    public static HttpRequest post(final CharSequence baseUrl,
                                   final Map<?, ?> params) {
        return post(baseUrl, params, true);
    }

    /**
     * Start a 'POST' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  the query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static HttpRequest post(final CharSequence baseUrl,
                                   final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return post(encode ? encode(url) : url);
    }

    /**
     * Start a 'POST' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the
     *                baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static HttpRequest post(final CharSequence baseUrl,
                                   final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return post(encode ? encode(url) : url);
    }

    /**
     * Start a 'PUT' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest put(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_PUT);
    }

    /**
     * Start a 'PUT' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest put(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_PUT);
    }

    /**
     * Start a 'PUT' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  the query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static HttpRequest put(final CharSequence baseUrl,
                                  final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return put(encode ? encode(url) : url);
    }

    /**
     * Start a 'PUT' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the
     *                baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static HttpRequest put(final CharSequence baseUrl,
                                  final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return put(encode ? encode(url) : url);
    }

    /**
     * Start a 'DELETE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest delete(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_DELETE);
    }

    /**
     * Start a 'DELETE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest delete(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_DELETE);
    }

    /**
     * Start a 'DELETE' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  The query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static HttpRequest delete(final CharSequence baseUrl,
                                     final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return delete(encode ? encode(url) : url);
    }

    /**
     * Start a 'DELETE' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the
     *                baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static HttpRequest delete(final CharSequence baseUrl,
                                     final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return delete(encode ? encode(url) : url);
    }

    /**
     * Start a 'HEAD' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest head(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_HEAD);
    }

    /**
     * Start a 'HEAD' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest head(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_HEAD);
    }

    public static HttpRequest head(final CharSequence baseUrl, final Map<?, ?> params) {
        return head(baseUrl, params, true);
    }

    /**
     * Start a 'HEAD' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  The query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static HttpRequest head(final CharSequence baseUrl,
                                   final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return head(encode ? encode(url) : url);
    }

    /**
     * Start a 'GET' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the
     *                baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static HttpRequest head(final CharSequence baseUrl,
                                   final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return head(encode ? encode(url) : url);
    }

    /**
     * Start an 'OPTIONS' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest options(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_OPTIONS);
    }

    /**
     * Start an 'OPTIONS' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest options(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_OPTIONS);
    }

    /**
     * Start a 'TRACE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest trace(final CharSequence url)
            throws HttpRequestException {
        return new HttpRequest(url, METHOD_TRACE);
    }

    /**
     * Start a 'TRACE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static HttpRequest trace(final URL url) throws HttpRequestException {
        return new HttpRequest(url, METHOD_TRACE);
    }

    /**
     * Set the 'http.keepAlive' property to the given value.
     * <p>
     * This setting will apply to all requests.
     *
     * @param keepAlive
     */
    public static void keepAlive(final boolean keepAlive) {
        setProperty("http.keepAlive", Boolean.toString(keepAlive));
    }

    /**
     * Set the 'http.maxConnections' property to the given value.
     * <p>
     * This setting will apply to all requests.
     *
     * @param maxConnections
     */
    public static void maxConnections(final int maxConnections) {
        setProperty("http.maxConnections", Integer.toString(maxConnections));
    }

    /**
     * Set the 'http.proxyHost' and 'https.proxyHost' properties to the given host
     * value.
     * <p>
     * This setting will apply to all requests.
     *
     * @param host
     */
    public static void proxyHost(final String host) {
        setProperty("http.proxyHost", host);
        setProperty("https.proxyHost", host);
    }

    /**
     * Set the 'http.proxyPort' and 'https.proxyPort' properties to the given port
     * number.
     * <p>
     * This setting will apply to all requests.
     *
     * @param port
     */
    public static void proxyPort(final int port) {
        final String portValue = Integer.toString(port);
        setProperty("http.proxyPort", portValue);
        setProperty("https.proxyPort", portValue);
    }

    /**
     * Set the 'http.nonProxyHosts' property to the given host values.
     * <p>
     * Hosts will be separated by a '|' character.
     * <p>
     * This setting will apply to all requests.
     *
     * @param hosts
     */
    public static void nonProxyHosts(final String... hosts) {
        if (hosts != null && hosts.length > 0) {
            StringBuilder separated = new StringBuilder();
            int last = hosts.length - 1;
            for (int i = 0; i < last; i++)
                separated.append(hosts[i]).append('|');
            separated.append(hosts[last]);
            setProperty("http.nonProxyHosts", separated.toString());
        } else
            setProperty("http.nonProxyHosts", null);
    }

    /**
     * Set property to given value.
     * <p>
     * Specifying a null value will cause the property to be cleared
     *
     * @param name
     * @param value
     * @return previous value
     */
    private static String setProperty(final String name, final String value) {
        final PrivilegedAction<String> action;
        if (value != null)
            action = new PrivilegedAction<String>() {

                public String run() {
                    return System.setProperty(name, value);
                }
            };
        else
            action = new PrivilegedAction<String>() {

                public String run() {
                    return System.clearProperty(name);
                }
            };
        return AccessController.doPrivileged(action);
    }

    private HttpURLConnection connection = null;

    private final URL url;

    private final String requestMethod;

    private RequestOutputStream output;

    private boolean multipart;

    private boolean form;

    private boolean ignoreCloseExceptions = true;

    private boolean uncompress = false;

    private int bufferSize = 8192;

    private long totalSize = -1;

    private long totalWritten = 0;

    private String httpProxyHost;

    private int httpProxyPort;

    private UploadProgress progress = UploadProgress.DEFAULT;

    /**
     * Create HTTP connection wrapper
     *
     * @param url    Remote resource URL.
     * @param method HTTP request method (e.g., "GET", "POST").
     * @throws HttpRequestException
     */
    public HttpRequest(final CharSequence url, final String method)
            throws HttpRequestException {
        try {
            this.url = new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new HttpRequestException(e);
        }
        this.requestMethod = method;
    }

    /**
     * Create HTTP connection wrapper
     *
     * @param url    Remote resource URL.
     * @param method HTTP request method (e.g., "GET", "POST").
     * @throws HttpRequestException
     */
    public HttpRequest(final URL url, final String method)
            throws HttpRequestException {
        this.url = url;
        this.requestMethod = method;
    }

    private Proxy createProxy() {
        return new Proxy(HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
    }

    private HttpURLConnection createConnection() {
        try {
            final HttpURLConnection connection;
            if (httpProxyHost != null)
                connection = CONNECTION_FACTORY.create(url, createProxy());
            else
                connection = CONNECTION_FACTORY.create(url);

//            //2023年5月30日 判断是否是https，跳过证书认证，但安全性下降，用户自行决定调用trustAllCerts()、trustAllHosts()
//            if("https".equalsIgnoreCase(url.getProtocol())){
//                ((HttpsURLConnection) connection).setSSLSocketFactory(getTrustedFactory());
//                ((HttpsURLConnection) connection).setHostnameVerifier(getTrustedVerifier());
//            }

            connection.setRequestMethod(requestMethod);
            log.debug("[{}]-[{}] connection:{}", requestMethod, connection.hashCode(), url.toString());
            return connection;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    @Override
    public String toString() {
        return method() + ' ' + url();
    }

    /**
     * Get underlying connection
     *
     * @return connection
     */
    public HttpURLConnection getConnection() {
        if (connection == null)
            connection = createConnection();
        return connection;
    }

    /**
     * Set whether or not to ignore exceptions that occur from calling
     * {@link Closeable#close()}
     * <p>
     * The default value of this setting is <code>true</code>
     *
     * @param ignore
     * @return this request
     */
    public HttpRequest ignoreCloseExceptions(final boolean ignore) {
        ignoreCloseExceptions = ignore;
        return this;
    }

    /**
     * Get whether or not exceptions thrown by {@link Closeable#close()} are
     * ignored
     *
     * @return true if ignoring, false if throwing
     */
    public boolean ignoreCloseExceptions() {
        return ignoreCloseExceptions;
    }

    /**
     * Get the status code of the response
     *
     * @return the response code
     * @throws HttpRequestException
     */
    public int code() throws HttpRequestException {
        try {
            closeOutput();
            int respCode = getConnection().getResponseCode();
            log.debug("[{}]-[{}] code:{}", requestMethod, getConnection().hashCode(), respCode);
            return respCode;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Set the value of the given {@link AtomicInteger} to the status code of the
     * response
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest code(final AtomicInteger output)
            throws HttpRequestException {
        output.set(code());
        return this;
    }

    /**
     * Is the response code a 200 OK?
     *
     * @return true if 200, false otherwise
     * @throws HttpRequestException
     */
    public boolean ok() throws HttpRequestException {
        return HTTP_OK == code();
    }

    /**
     * Is the response code a 201 Created?
     *
     * @return true if 201, false otherwise
     * @throws HttpRequestException
     */
    public boolean created() throws HttpRequestException {
        return HTTP_CREATED == code();
    }

    /**
     * Is the response code a 204 No Content?
     *
     * @return true if 204, false otherwise
     * @throws HttpRequestException
     */
    public boolean noContent() throws HttpRequestException {
        return HTTP_NO_CONTENT == code();
    }

    /**
     * Is the response code a 500 Internal Server Error?
     *
     * @return true if 500, false otherwise
     * @throws HttpRequestException
     */
    public boolean serverError() throws HttpRequestException {
        return HTTP_INTERNAL_ERROR == code();
    }

    /**
     * Is the response code a 400 Bad Request?
     *
     * @return true if 400, false otherwise
     * @throws HttpRequestException
     */
    public boolean badRequest() throws HttpRequestException {
        return HTTP_BAD_REQUEST == code();
    }

    /**
     * Is the response code a 404 Not Found?
     *
     * @return true if 404, false otherwise
     * @throws HttpRequestException
     */
    public boolean notFound() throws HttpRequestException {
        return HTTP_NOT_FOUND == code();
    }

    /**
     * Is the response code a 304 Not Modified?
     *
     * @return true if 304, false otherwise
     * @throws HttpRequestException
     */
    public boolean notModified() throws HttpRequestException {
        return HTTP_NOT_MODIFIED == code();
    }

    /**
     * Get status message of the response
     *
     * @return message
     * @throws HttpRequestException
     */
    public String message() throws HttpRequestException {
        try {
            closeOutput();
            String respMessage = getConnection().getResponseMessage();
            log.debug("[{}]-[{}] message:{}", requestMethod, getConnection().hashCode(), message());
            return respMessage;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Disconnect the connection
     *
     * @return this request
     */
    public HttpRequest disconnect() {
        log.debug("[{}]-[{}] disconnect", requestMethod, getConnection().hashCode());
        getConnection().disconnect();
        return this;
    }

    /**
     * Set chunked streaming mode to the given size
     *
     * @param size
     * @return this request
     */
    public HttpRequest chunk(final int size) {
        getConnection().setChunkedStreamingMode(size);
        return this;
    }

    /**
     * Set the size used when buffering and copying between streams
     * <p>
     * This size is also used for send and receive buffers created for both char
     * and byte arrays
     * <p>
     * The default buffer size is 8,192 bytes
     *
     * @param size
     * @return this request
     */
    public HttpRequest bufferSize(final int size) {
        if (size < 1)
            throw new IllegalArgumentException("Size must be greater than zero");
        bufferSize = size;
        return this;
    }

    /**
     * Get the configured buffer size
     * <p>
     * The default buffer size is 8,192 bytes
     *
     * @return buffer size
     */
    public int bufferSize() {
        return bufferSize;
    }

    /**
     * Set whether or not the response body should be automatically uncompressed
     * when read from.
     * <p>
     * This will only affect requests that have the 'Content-Encoding' response
     * header set to 'gzip'.
     * <p>
     * This causes all receive methods to use a {@link GZIPInputStream} when
     * applicable so that higher level streams and readers can read the data
     * uncompressed.
     * <p>
     * Setting this option does not cause any request headers to be set
     * automatically so {@link #acceptGzipEncoding()} should be used in
     * conjunction with this setting to tell the server to gzip the response.
     *
     * @param uncompress
     * @return this request
     */
    public HttpRequest uncompress(final boolean uncompress) {
        this.uncompress = uncompress;
        return this;
    }

    /**
     * Create byte array output stream
     *
     * @return stream
     */
    protected ByteArrayOutputStream byteStream() {
        final int size = contentLength();
        if (size > 0)
            return new ByteArrayOutputStream(size);
        else
            return new ByteArrayOutputStream();
    }

    /**
     * Get response as {@link String} in given character set
     * <p>
     * This will fall back to using the UTF-8 character set if the given charset
     * is null
     *
     * @param charset
     * @return string
     * @throws HttpRequestException
     */
    public String body(final String charset) throws HttpRequestException {
        final ByteArrayOutputStream output = byteStream();
        try {
            copy(buffer(), output);
            String resp = output.toString(getValidCharset(charset));
            if (log.isDebugEnabled()) {
                String str = resp
                        // 去掉换行
                        .replaceAll("\r\n|\r|\n", " ")
                        // 将tab、空格进行折叠
//                        .replaceAll("\t", " ")
//                        .replaceAll("            ", " ")
//                        .replaceAll("           ", " ")
//                        .replaceAll("          ", " ")
//                        .replaceAll("         ", " ")
//                        .replaceAll("        ", " ")
//                        .replaceAll("       "," ")
//                        .replaceAll("      "," ")
//                        .replaceAll("     "," ")
//                        .replaceAll("    ", " ")
//                        .replaceAll("   "," ")
//                        .replaceAll("  ", " ")
                        // replaceAll支持正则表达式，\\s+一个或多个连续的空格都会替换成一个空格
                        .replaceAll("\\s+"," ")
                        ;
                log.debug("[{}]-[{}] response:{}", requestMethod, getConnection().hashCode(), str.length() > 512 ? str.substring(0, 512) + "..." : str);
            }
            return resp;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Get response as {@link String} using character set returned from
     * {@link #charset()}
     *
     * @return string
     * @throws HttpRequestException
     */
    public String body() throws HttpRequestException {
        return body(charset());
    }

    /**
     * Get the response body as a {@link String} and set it as the value of the
     * given reference.
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest body(final AtomicReference<String> output) throws HttpRequestException {
        output.set(body());
        return this;
    }

    /**
     * Get the response body as a {@link String} and set it as the value of the
     * given reference.
     *
     * @param output
     * @param charset
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest body(final AtomicReference<String> output, final String charset) throws HttpRequestException {
        output.set(body(charset));
        return this;
    }


    /**
     * Is the response body empty?
     *
     * @return true if the Content-Length response header is 0, false otherwise
     * @throws HttpRequestException
     */
    public boolean isBodyEmpty() throws HttpRequestException {
        return contentLength() == 0;
    }

    /**
     * Get response as byte array
     *
     * @return byte array
     * @throws HttpRequestException
     */
    public byte[] bytes() throws HttpRequestException {
        final ByteArrayOutputStream output = byteStream();
        try {
            copy(buffer(), output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return output.toByteArray();
    }

    /**
     * Get response in a buffered stream
     *
     * @return stream
     * @throws HttpRequestException
     * @see #bufferSize(int)
     */
    public BufferedInputStream buffer() throws HttpRequestException {
        return new BufferedInputStream(stream(), bufferSize);
    }

    /**
     * Get stream to response body
     *
     * @return stream
     * @throws HttpRequestException
     */
    public InputStream stream() throws HttpRequestException {
        InputStream stream;
        if (code() < HTTP_BAD_REQUEST)
            try {
                stream = getConnection().getInputStream();
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
        else {
            try {
                log.error("[{}]-[{}] message:{}", requestMethod, getConnection().hashCode(), getConnection().getResponseMessage());
            }catch (IOException e){
                throw new HttpRequestException(e);
            }

            stream = getConnection().getErrorStream();
            if (stream == null)
                try {
                    stream = getConnection().getInputStream();
                } catch (IOException e) {
                    if (contentLength() > 0)
                        throw new HttpRequestException(e);
                    else
                        stream = new ByteArrayInputStream(new byte[0]);
                }
        }

        if (!uncompress || !ENCODING_GZIP.equals(contentEncoding()))
            return stream;
        else
            try {
                return new GZIPInputStream(stream);
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
    }

    /**
     * Get reader to response body using given character set.
     * <p>
     * This will fall back to using the UTF-8 character set if the given charset
     * is null
     *
     * @param charset
     * @return reader
     * @throws HttpRequestException
     */
    public InputStreamReader reader(final String charset)
            throws HttpRequestException {
        try {
            return new InputStreamReader(stream(), getValidCharset(charset));
        } catch (UnsupportedEncodingException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Get reader to response body using the character set returned from
     * {@link #charset()}
     *
     * @return reader
     * @throws HttpRequestException
     */
    public InputStreamReader reader() throws HttpRequestException {
        return reader(charset());
    }

    /**
     * Get buffered reader to response body using the given character set r and
     * the configured buffer size
     *
     * @param charset
     * @return reader
     * @throws HttpRequestException
     * @see #bufferSize(int)
     */
    public BufferedReader bufferedReader(final String charset)
            throws HttpRequestException {
        return new BufferedReader(reader(charset), bufferSize);
    }

    /**
     * Get buffered reader to response body using the character set returned from
     * {@link #charset()} and the configured buffer size
     *
     * @return reader
     * @throws HttpRequestException
     * @see #bufferSize(int)
     */
    public BufferedReader bufferedReader() throws HttpRequestException {
        return bufferedReader(charset());
    }

    /**
     * 下载响应内容到文件
     *
     * @param path 文件夹地址、文件夹地址+自定义文件名；注意文件夹地址必须存在，不提供自动创建
     * @return
     * @throws HttpRequestException
     */
    public HttpRequest download(final String path) throws HttpRequestException {
        File file = new File(path);
        // 自动创建文件夹
        if (!file.exists()) {
            boolean b = file.mkdirs();
            if (!b) {
                throw new RuntimeException("创建文件夹[" + path + "]失败");
            }
            log.debug("创建文件夹[{}]成功", path);
        }

        String dirPath = path;
        if (file.isDirectory()) {
            dirPath = file.getParent();
        }

        String downloadPath = dirPath.replaceAll("[/\\\\]", Matcher.quoteReplacement(File.separator));
        if (!downloadPath.endsWith(File.separator)) {
            downloadPath += File.separator;
        }

        String u = this.url().getPath();
        String fileName = u.substring(u.lastIndexOf("/") + 1);

        return receive(new File(downloadPath + fileName));
    }

    /**
     * Stream response body to file
     *
     * @param file
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final File file) throws HttpRequestException {
        log.debug("[{}] download:{}", requestMethod, file.getAbsolutePath());
        final OutputStream output;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
        } catch (FileNotFoundException e) {
            throw new HttpRequestException(e);
        }
        return new CloseOperation<HttpRequest>(output, ignoreCloseExceptions) {

            @Override
            protected HttpRequest run() throws HttpRequestException, IOException {
                return receive(output);
            }
        }.call();
    }

    /**
     * Stream response to given output stream
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final OutputStream output)
            throws HttpRequestException {
        try {
            return copy(buffer(), output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Stream response to given print stream
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final PrintStream output)
            throws HttpRequestException {
        return receive((OutputStream) output);
    }

    /**
     * Receive response into the given appendable
     *
     * @param appendable
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final Appendable appendable)
            throws HttpRequestException {
        final BufferedReader reader = bufferedReader();
        return new CloseOperation<HttpRequest>(reader, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                final CharBuffer buffer = CharBuffer.allocate(bufferSize);
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    buffer.rewind();
                    appendable.append(buffer, 0, read);
                    buffer.rewind();
                }
                return HttpRequest.this;
            }
        }.call();
    }

    /**
     * Receive response into the given writer
     *
     * @param writer
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final Writer writer) throws HttpRequestException {
        final BufferedReader reader = bufferedReader();
        return new CloseOperation<HttpRequest>(reader, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                return copy(reader, writer);
            }
        }.call();
    }

    /**
     * Set read timeout on connection to given value
     *
     * @param timeout
     * @return this request
     */
    public HttpRequest readTimeout(final int timeout) {
        getConnection().setReadTimeout(timeout);
        return this;
    }

    /**
     * Set connect timeout on connection to given value
     *
     * @param timeout
     * @return this request
     */
    public HttpRequest connectTimeout(final int timeout) {
        getConnection().setConnectTimeout(timeout);
        return this;
    }

    /**
     * Set header name to given value
     *
     * @param name
     * @param value
     * @return this request
     */
    public HttpRequest header(final String name, final String value) {
        HttpURLConnection conn = getConnection();
        if (log.isDebugEnabled()
                && (HEADER_USER_AGENT.equals(name)
                || HEADER_COOKIE.equalsIgnoreCase(name))
                || HEADER_CONTENT_TYPE.equalsIgnoreCase(name)
                || HEADER_AUTHORIZATION.equalsIgnoreCase(name)
        ) {
            log.debug("[{}]-[{}] header:{}={}", requestMethod, conn.hashCode(), name, value);
        }
        conn.setRequestProperty(name, value);
        return this;
    }

    /**
     * Set header name to given value
     *
     * @param name
     * @param value
     * @return this request
     */
    public HttpRequest header(final String name, final Number value) {
        return header(name, value != null ? value.toString() : null);
    }

    /**
     * Set all headers found in given map where the keys are the header names and
     * the values are the header values
     *
     * @param headers
     * @return this request
     */
    public HttpRequest headers(final Map<String, String> headers) {
        if (!headers.isEmpty())
            for (Entry<String, String> header : headers.entrySet())
                header(header);
        return this;
    }

    /**
     * Set header to have given entry's key as the name and value as the value
     *
     * @param header
     * @return this request
     */
    public HttpRequest header(final Entry<String, String> header) {
        return header(header.getKey(), header.getValue());
    }

    /**
     * Get a response header
     *
     * @param name
     * @return response header
     * @throws HttpRequestException
     */
    public String header(final String name) throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderField(name);
    }

    /**
     * Get all the response headers
     *
     * @return map of response header names to their value(s)
     * @throws HttpRequestException
     */
    public Map<String, List<String>> headers() throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFields();
    }

    /**
     * Get a date header from the response falling back to returning -1 if the
     * header is missing or parsing fails
     *
     * @param name
     * @return date, -1 on failures
     * @throws HttpRequestException
     */
    public long dateHeader(final String name) throws HttpRequestException {
        return dateHeader(name, -1L);
    }

    /**
     * Get a date header from the response falling back to returning the given
     * default value if the header is missing or parsing fails
     *
     * @param name
     * @param defaultValue
     * @return date, default value on failures
     * @throws HttpRequestException
     */
    public long dateHeader(final String name, final long defaultValue)
            throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFieldDate(name, defaultValue);
    }

    /**
     * Get an integer header from the response falling back to returning -1 if the
     * header is missing or parsing fails
     *
     * @param name
     * @return header value as an integer, -1 when missing or parsing fails
     * @throws HttpRequestException
     */
    public int intHeader(final String name) throws HttpRequestException {
        return intHeader(name, -1);
    }

    /**
     * Get an integer header value from the response falling back to the given
     * default value if the header is missing or if parsing fails
     *
     * @param name
     * @param defaultValue
     * @return header value as an integer, default value when missing or parsing
     * fails
     * @throws HttpRequestException
     */
    public int intHeader(final String name, final int defaultValue)
            throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFieldInt(name, defaultValue);
    }

    /**
     * Get all values of the given header from the response
     *
     * @param name
     * @return non-null but possibly empty array of {@link String} header values
     */
    public String[] headers(final String name) {
        final Map<String, List<String>> headers = headers();
        if (headers == null || headers.isEmpty())
            return EMPTY_STRINGS;

        final List<String> values = headers.get(name);
        if (values != null && !values.isEmpty())
            return values.toArray(new String[values.size()]);
        else
            return EMPTY_STRINGS;
    }

    public HttpRequest cookies(String value) {
        if (null != value && value.trim().length() > 0) {
            header(HEADER_COOKIE, value);
        }
        return this;
    }

    public HttpRequest cookies(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : map.entrySet()) {
            sb.append(";");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return cookies(sb.substring(1));
    }

    public String cookies2() {
        String[] cookies = headers("Set-Cookie");

        StringBuilder cookiesSb = new StringBuilder();
        for (String cookie : cookies) {
            String[] items = cookie.split(";");
            for (String item : items) {
                String str = item.trim();
                if (str.indexOf("=") > 0) {
                    int pos = str.indexOf("=");
                    String key = str.substring(0, pos);
                    if ("path".equals(key)
                            //|| "maxAge".equals(key)
                            || "expires".equals(key)
                            || "domain".equals(key)
                            || "secure".equals(key)
                            || "httpOnly".equals(key)
                            || "overwrite".equals(key)
                    ) {
                        continue;
                    }
                    String value = str.substring(pos + 1);
                    cookiesSb.append(";").append(key).append("=").append(value);
                }
            }
        }

        String str = cookiesSb.substring(2);
        log.debug("[{}]-[{}] Set-Cookie:{}", requestMethod, getConnection().hashCode(), str);
        return str;
    }

    public Map<String, String> cookies() {
        String[] cookies = headers("Set-Cookie");

        Map<String, String> map = new HashMap<>();
        for (String cookie : cookies) {
            String[] items = cookie.split(";");
            for (String item : items) {
                String str = item.trim();
                if (str.indexOf("=") > 0) {
                    int pos = str.indexOf("=");
                    String key = str.substring(0, pos);
                    if ("path".equals(key)
                            //|| "maxAge".equals(key)
                            || "expires".equals(key)
                            || "domain".equals(key)
                            || "secure".equals(key)
                            || "httpOnly".equals(key)
                            || "overwrite".equals(key)
                    ) {
                        continue;
                    }
                    String value = str.substring(pos + 1);
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    /**
     * Get parameter with given name from header value in response
     *
     * @param headerName
     * @param paramName
     * @return parameter value or null if missing
     */
    public String parameter(final String headerName, final String paramName) {
        return getParam(header(headerName), paramName);
    }

    /**
     * Get all parameters from header value in response
     * <p>
     * This will be all key=value pairs after the first ';' that are separated by
     * a ';'
     *
     * @param headerName
     * @return non-null but possibly empty map of parameter headers
     */
    public Map<String, String> parameters(final String headerName) {
        return getParams(header(headerName));
    }

    /**
     * Get parameter values from header value
     *
     * @param header
     * @return parameter value or null if none
     */
    protected Map<String, String> getParams(final String header) {
        if (header == null || header.length() == 0)
            return Collections.emptyMap();

        final int headerLength = header.length();
        int start = header.indexOf(';') + 1;
        if (start == 0 || start == headerLength)
            return Collections.emptyMap();

        int end = header.indexOf(';', start);
        if (end == -1)
            end = headerLength;

        Map<String, String> params = new LinkedHashMap<String, String>();
        while (start < end) {
            int nameEnd = header.indexOf('=', start);
            if (nameEnd != -1 && nameEnd < end) {
                String name = header.substring(start, nameEnd).trim();
                if (name.length() > 0) {
                    String value = header.substring(nameEnd + 1, end).trim();
                    int length = value.length();
                    if (length != 0)
                        if (length > 2 && '"' == value.charAt(0)
                                && '"' == value.charAt(length - 1))
                            params.put(name, value.substring(1, length - 1));
                        else
                            params.put(name, value);
                }
            }

            start = end + 1;
            end = header.indexOf(';', start);
            if (end == -1)
                end = headerLength;
        }

        return params;
    }

    /**
     * Get parameter value from header value
     *
     * @param value
     * @param paramName
     * @return parameter value or null if none
     */
    protected String getParam(final String value, final String paramName) {
        if (value == null || value.length() == 0)
            return null;

        final int length = value.length();
        int start = value.indexOf(';') + 1;
        if (start == 0 || start == length)
            return null;

        int end = value.indexOf(';', start);
        if (end == -1)
            end = length;

        while (start < end) {
            int nameEnd = value.indexOf('=', start);
            if (nameEnd != -1 && nameEnd < end
                    && paramName.equals(value.substring(start, nameEnd).trim())) {
                String paramValue = value.substring(nameEnd + 1, end).trim();
                int valueLength = paramValue.length();
                if (valueLength != 0)
                    if (valueLength > 2 && '"' == paramValue.charAt(0)
                            && '"' == paramValue.charAt(valueLength - 1))
                        return paramValue.substring(1, valueLength - 1);
                    else
                        return paramValue;
            }

            start = end + 1;
            end = value.indexOf(';', start);
            if (end == -1)
                end = length;
        }

        return null;
    }

    /**
     * Get 'charset' parameter from 'Content-Type' response header
     *
     * @return charset or null if none
     */
    public String charset() {
        return parameter(HEADER_CONTENT_TYPE, PARAM_CHARSET);
    }

    /**
     * Set the 'User-Agent' header to given value
     *
     * @param userAgent
     * @return this request
     */
    public HttpRequest userAgent(final String userAgent) {
        return header(HEADER_USER_AGENT, userAgent);
    }

    /**
     * Set the 'Referer' header to given value
     *
     * @param referer
     * @return this request
     */
    public HttpRequest referer(final String referer) {
        return header(HEADER_REFERER, referer);
    }

    /**
     * Set value of {@link HttpURLConnection#setUseCaches(boolean)}
     *
     * @param useCaches
     * @return this request
     */
    public HttpRequest useCaches(final boolean useCaches) {
        getConnection().setUseCaches(useCaches);
        return this;
    }

    /**
     * Set the 'Accept-Encoding' header to given value
     *
     * @param acceptEncoding
     * @return this request
     */
    public HttpRequest acceptEncoding(final String acceptEncoding) {
        return header(HEADER_ACCEPT_ENCODING, acceptEncoding);
    }

    /**
     * Set the 'Accept-Encoding' header to 'gzip'
     *
     * @return this request
     * @see #uncompress(boolean)
     */
    public HttpRequest acceptGzipEncoding() {
        return acceptEncoding(ENCODING_GZIP);
    }

    /**
     * Set the 'Accept-Charset' header to given value
     *
     * @param acceptCharset
     * @return this request
     */
    public HttpRequest acceptCharset(final String acceptCharset) {
        return header(HEADER_ACCEPT_CHARSET, acceptCharset);
    }

    /**
     * Get the 'Content-Encoding' header from the response
     *
     * @return this request
     */
    public String contentEncoding() {
        return header(HEADER_CONTENT_ENCODING);
    }

    /**
     * Get the 'Server' header from the response
     *
     * @return server
     */
    public String server() {
        return header(HEADER_SERVER);
    }

    /**
     * Get the 'Date' header from the response
     *
     * @return date value, -1 on failures
     */
    public long date() {
        return dateHeader(HEADER_DATE);
    }

    /**
     * Get the 'Cache-Control' header from the response
     *
     * @return cache control
     */
    public String cacheControl() {
        return header(HEADER_CACHE_CONTROL);
    }

    /**
     * Get the 'ETag' header from the response
     *
     * @return entity tag
     */
    public String eTag() {
        return header(HEADER_ETAG);
    }

    /**
     * Get the 'Expires' header from the response
     *
     * @return expires value, -1 on failures
     */
    public long expires() {
        return dateHeader(HEADER_EXPIRES);
    }

    /**
     * Get the 'Last-Modified' header from the response
     *
     * @return last modified value, -1 on failures
     */
    public long lastModified() {
        return dateHeader(HEADER_LAST_MODIFIED);
    }

    /**
     * Get the 'Location' header from the response
     *
     * @return location
     */
    public String location() {
        return header(HEADER_LOCATION);
    }

    /**
     * Set the 'Authorization' header to given value
     *
     * @param authorization
     * @return this request
     */
    public HttpRequest authorization(final String authorization) {
        return header(HEADER_AUTHORIZATION, authorization);
    }

    /**
     * Set the 'Proxy-Authorization' header to given value
     *
     * @param proxyAuthorization
     * @return this request
     */
    public HttpRequest proxyAuthorization(final String proxyAuthorization) {
        return header(HEADER_PROXY_AUTHORIZATION, proxyAuthorization);
    }

    /**
     * Set the 'Authorization' header to given values in Basic authentication
     * format
     *
     * @param name
     * @param password
     * @return this request
     */
    public HttpRequest basic(final String name, final String password) {
        return authorization("Basic " + Base64.encode(name + ':' + password));
    }

    /**
     * Set the 'Proxy-Authorization' header to given values in Basic authentication
     * format
     *
     * @param name
     * @param password
     * @return this request
     */
    public HttpRequest proxyBasic(final String name, final String password) {
        return proxyAuthorization("Basic " + Base64.encode(name + ':' + password));
    }

    /**
     * Bearer 授权机制是一种基于令牌的授权方式，它使用一个令牌来代替用户名和密码。
     * 这个令牌是由授权服务器颁发的，通常是通过OAuth 2.0协议来实现。
     * Bearer令牌具有一定的时效性，一旦失效，就需要重新获取。
     *
     * @param token
     * @return
     */
    public HttpRequest bearer(final String token) {
        return authorization(String.format("Bearer %s", token));
    }

    /**
     * Set the 'If-Modified-Since' request header to the given value
     *
     * @param ifModifiedSince
     * @return this request
     */
    public HttpRequest ifModifiedSince(final long ifModifiedSince) {
        getConnection().setIfModifiedSince(ifModifiedSince);
        return this;
    }

    /**
     * Set the 'If-None-Match' request header to the given value
     *
     * @param ifNoneMatch
     * @return this request
     */
    public HttpRequest ifNoneMatch(final String ifNoneMatch) {
        return header(HEADER_IF_NONE_MATCH, ifNoneMatch);
    }

    /**
     * Set the 'Content-Type' request header to the given value
     *
     * @param contentType
     * @return this request
     */
    public HttpRequest contentType(final String contentType) {
        return contentType(contentType, null);
    }

    /**
     * Set the 'Content-Type' request header to the given value and charset
     *
     * @param contentType
     * @param charset
     * @return this request
     */
    public HttpRequest contentType(final String contentType, final String charset) {
        if (charset != null && charset.length() > 0) {
            final String separator = "; " + PARAM_CHARSET + '=';
            return header(HEADER_CONTENT_TYPE, contentType + separator + charset);
        } else
            return header(HEADER_CONTENT_TYPE, contentType);
    }

    /**
     * Get the 'Content-Type' header from the response
     *
     * @return response header value
     */
    public String contentType() {
        return header(HEADER_CONTENT_TYPE);
    }

    /**
     * Get the 'Content-Length' header from the response
     *
     * @return response header value
     */
    public int contentLength() {
        return intHeader(HEADER_CONTENT_LENGTH);
    }

    /**
     * Set the 'Content-Length' request header to the given value
     *
     * @param contentLength
     * @return this request
     */
    public HttpRequest contentLength(final String contentLength) {
        return contentLength(Integer.parseInt(contentLength));
    }

    /**
     * Set the 'Content-Length' request header to the given value
     *
     * @param contentLength
     * @return this request
     */
    public HttpRequest contentLength(final int contentLength) {
        getConnection().setFixedLengthStreamingMode(contentLength);
        return this;
    }

    /**
     * Set the 'Accept' header to given value
     *
     * @param accept
     * @return this request
     */
    public HttpRequest accept(final String accept) {
        return header(HEADER_ACCEPT, accept);
    }

    /**
     * Set the 'Accept' header to 'application/json'
     *
     * @return this request
     */
    public HttpRequest acceptJson() {
        return accept(CONTENT_TYPE_JSON);
    }

    /**
     * Copy from input stream to output stream
     *
     * @param input
     * @param output
     * @return this request
     * @throws IOException
     */
    protected HttpRequest copy(final InputStream input, final OutputStream output)
            throws IOException {
        return new CloseOperation<HttpRequest>(input, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                final byte[] buffer = new byte[bufferSize];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalWritten += read;
                    progress.onUpload(totalWritten, totalSize);
                }
                return HttpRequest.this;
            }
        }.call();
    }

    /**
     * Copy from reader to writer
     *
     * @param input
     * @param output
     * @return this request
     * @throws IOException
     */
    protected HttpRequest copy(final Reader input, final Writer output)
            throws IOException {
        return new CloseOperation<HttpRequest>(input, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                final char[] buffer = new char[bufferSize];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalWritten += read;
                    progress.onUpload(totalWritten, -1);
                }
                return HttpRequest.this;
            }
        }.call();
    }

    /**
     * Set the UploadProgress callback for this request
     *
     * @param callback
     * @return this request
     */
    public HttpRequest progress(final UploadProgress callback) {
        if (callback == null)
            progress = UploadProgress.DEFAULT;
        else
            progress = callback;
        return this;
    }

    private HttpRequest incrementTotalSize(final long size) {
        if (totalSize == -1)
            totalSize = 0;
        totalSize += size;
        return this;
    }

    /**
     * Close output stream
     *
     * @return this request
     * @throws HttpRequestException
     * @throws IOException
     */
    protected HttpRequest closeOutput() throws IOException {
        progress(null);
        if (output == null)
            return this;
        if (multipart)
            output.write(CRLF + "--" + BOUNDARY + "--" + CRLF);
        if (ignoreCloseExceptions)
            try {
                output.close();
            } catch (IOException ignored) {
                // Ignored
            }
        else
            output.close();
        output = null;
        return this;
    }

    /**
     * Call {@link #closeOutput()} and re-throw a caught {@link IOException}s as
     * an {@link HttpRequestException}
     *
     * @return this request
     * @throws HttpRequestException
     */
    protected HttpRequest closeOutputQuietly() throws HttpRequestException {
        try {
            return closeOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Open output stream
     *
     * @return this request
     * @throws IOException
     */
    protected HttpRequest openOutput() throws IOException {
        if (output != null)
            return this;
        getConnection().setDoOutput(true);
        final String charset = getParam(
                getConnection().getRequestProperty(HEADER_CONTENT_TYPE), PARAM_CHARSET);
        output = new RequestOutputStream(getConnection().getOutputStream(), charset,
                bufferSize);
        return this;
    }

    /**
     * Start part of a multipart
     *
     * @return this request
     * @throws IOException
     */
    protected HttpRequest startPart() throws IOException {
        if (!multipart) {
            multipart = true;
            contentType(CONTENT_TYPE_MULTIPART).openOutput();
            output.write("--" + BOUNDARY + CRLF);
        } else
            output.write(CRLF + "--" + BOUNDARY + CRLF);
        return this;
    }

    /**
     * Write part header
     *
     * @param name
     * @param filename
     * @return this request
     * @throws IOException
     */
    protected HttpRequest writePartHeader(final String name, final String filename)
            throws IOException {
        return writePartHeader(name, filename, null);
    }

    /**
     * Write part header
     *
     * @param name
     * @param filename
     * @param contentType
     * @return this request
     * @throws IOException
     */
    protected HttpRequest writePartHeader(final String name,
                                          final String filename, final String contentType) throws IOException {
        final StringBuilder partBuffer = new StringBuilder();
        partBuffer.append("form-data; name=\"").append(name);
        if (filename != null)
            partBuffer.append("\"; filename=\"").append(filename);
        partBuffer.append('"');
        partHeader("Content-Disposition", partBuffer.toString());
        if (contentType != null)
            partHeader(HEADER_CONTENT_TYPE, contentType);
        return send(CRLF);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param part
     * @return this request
     */
    public HttpRequest part(final String name, final String part) {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param filename
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final String filename,
                            final String part) throws HttpRequestException {
        return part(name, filename, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param filename
     * @param contentType value of the Content-Type part header
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final String filename,
                            final String contentType, final String part) throws HttpRequestException {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            output.write(part);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final Number part)
            throws HttpRequestException {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param filename
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final String filename,
                            final Number part) throws HttpRequestException {
        return part(name, filename, part != null ? part.toString() : null);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final File part)
            throws HttpRequestException {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param filename
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final String filename,
                            final File part) throws HttpRequestException {
        return part(name, filename, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param filename
     * @param contentType value of the Content-Type part header
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final String filename,
                            final String contentType, final File part) throws HttpRequestException {
        final InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(part));
            incrementTotalSize(part.length());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return part(name, filename, contentType, stream);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final InputStream part)
            throws HttpRequestException {
        return part(name, null, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     *
     * @param name
     * @param filename
     * @param contentType value of the Content-Type part header
     * @param part
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest part(final String name, final String filename,
                            final String contentType, final InputStream part)
            throws HttpRequestException {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            copy(part, output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write a multipart header to the response body
     *
     * @param name
     * @param value
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest partHeader(final String name, final String value)
            throws HttpRequestException {
        return send(name).send(": ").send(value).send(CRLF);
    }

    /**
     * Write contents of file to request body
     *
     * @param input
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest send(final File input) throws HttpRequestException {
        final InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(input));
            incrementTotalSize(input.length());
        } catch (FileNotFoundException e) {
            throw new HttpRequestException(e);
        }
        log.debug("[{}]-[{}] file:{}[size:{},path={}]", requestMethod, getConnection().hashCode(), input.getName(), input.length(), input.getAbsolutePath());
        return send(stream);
    }

    /**
     * Write byte array to request body
     *
     * @param input
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest send(final byte[] input) throws HttpRequestException {
        if (input != null)
            incrementTotalSize(input.length);
        log.debug("[{}]-[{}] bytes:{}", requestMethod, getConnection().hashCode(), input.length);
        return send(new ByteArrayInputStream(input));
    }

    /**
     * Write stream to request body
     * <p>
     * The given stream will be closed once sending completes
     *
     * @param input
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest send(final InputStream input) throws HttpRequestException {
        try {
            openOutput();
            copy(input, output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write reader to request body
     * <p>
     * The given reader will be closed once sending completes
     *
     * @param input
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest send(final Reader input) throws HttpRequestException {
        try {
            openOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        final Writer writer = new OutputStreamWriter(output,
                output.encoder.charset());
        return new FlushOperation<HttpRequest>(writer) {

            @Override
            protected HttpRequest run() throws IOException {
                return copy(input, writer);
            }
        }.call();
    }

    /**
     * Write char sequence to request body
     * <p>
     * The charset configured via {@link #contentType(String)} will be used and
     * UTF-8 will be used if it is unset.
     *
     * @param value
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest send(final CharSequence value) throws HttpRequestException {
        try {
            openOutput();
            output.write(value.toString());
            log.debug("[{}]-[{}] data:{}", requestMethod, getConnection().hashCode(), value);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Create writer to request output stream
     *
     * @return writer
     * @throws HttpRequestException
     */
    public OutputStreamWriter writer() throws HttpRequestException {
        try {
            openOutput();
            return new OutputStreamWriter(output, output.encoder.charset());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Write the values in the map as form data to the request body
     * <p>
     * The pairs specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     *
     * @param values
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest form(final Map<?, ?> values) throws HttpRequestException {
        return form(values, CHARSET_UTF8);
    }

    /**
     * Write the key and value in the entry as form data to the request body
     * <p>
     * The pair specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     *
     * @param entry
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest form(final Entry<?, ?> entry) throws HttpRequestException {
        return form(entry, CHARSET_UTF8);
    }

    /**
     * Write the key and value in the entry as form data to the request body
     * <p>
     * The pair specified will be URL-encoded and sent with the
     * 'application/x-www-form-urlencoded' content-type
     *
     * @param entry
     * @param charset
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest form(final Entry<?, ?> entry, final String charset)
            throws HttpRequestException {
        return form(entry.getKey(), entry.getValue(), charset);
    }

    /**
     * Write the name/value pair as form data to the request body
     * <p>
     * The pair specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     *
     * @param name
     * @param value
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest form(final Object name, final Object value)
            throws HttpRequestException {
        return form(name, value, CHARSET_UTF8);
    }

    /**
     * Write the name/value pair as form data to the request body
     * <p>
     * The values specified will be URL-encoded and sent with the
     * 'application/x-www-form-urlencoded' content-type
     *
     * @param name
     * @param value
     * @param charset
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest form(final Object name, final Object value, String charset)
            throws HttpRequestException {
        final boolean first = !form;
        if (first) {
            contentType(CONTENT_TYPE_FORM, charset);
            form = true;
        }
        charset = getValidCharset(charset);
        try {
            openOutput();
            if (!first)
                output.write('&');
            output.write(URLEncoder.encode(name.toString(), charset));
            output.write('=');
            if (value != null)
                output.write(URLEncoder.encode(value.toString(), charset));
            log.debug("[{}]-[{}] form:{}={}", requestMethod, getConnection().hashCode(), URLEncoder.encode(name.toString(), charset), (null == value ? null : URLEncoder.encode(value.toString(), charset)));
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write the values in the map as encoded form data to the request body
     *
     * @param values
     * @param charset
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest form(final Map<?, ?> values, final String charset)
            throws HttpRequestException {
        if (null != values && !values.isEmpty())
            for (Entry<?, ?> entry : values.entrySet())
                form(entry, charset);
        return this;
    }

    public HttpRequest graphql(String query) {
        return graphql(query, null, null);
    }

    public HttpRequest graphql(String query, String variables, String operationName) {
        Map<String, String> params = new HashMap<>();
        params.put("query", query);
        if (null != variables && variables.trim().length() > 0) {
            params.put("variables", variables);
        }
        if (null != operationName && operationName.trim().length() > 0) {
            params.put("operationName", operationName);
        }

        if (METHOD_GET.equals(this.requestMethod)) {
            append(this.url.getPath(), params);
        } else if (METHOD_POST.equals(this.requestMethod)) {
            json(params);
        } else {
            throw new RuntimeException("Not Support [" + this.requestMethod + "] Use GraphQL");
        }
        return this;
    }

    public HttpRequest json(Map<?, ?> params) {
        return json(params, CHARSET_UTF8);
    }

    public HttpRequest json(Map<?, ?> params, final String charset) {
        return json(json2string(params), charset);
    }

    private static String json2string(Map<?, ?> json) {
        if (null == json) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (null != json && !json.isEmpty()) {
            for (Entry<?, ?> entry : json.entrySet()) {
                sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\"");
                sb.append(":");
                if (null == entry.getValue()) {
                    sb.append((String) null);
                } else if (entry.getValue().getClass().isPrimitive()) {
                    sb.append(entry.getValue());
                } else if (
                        entry.getValue().getClass() == Boolean.class
                                || entry.getValue().getClass() == Byte.class
                                || entry.getValue().getClass() == Short.class
                                || entry.getValue().getClass() == Integer.class
                                || entry.getValue().getClass() == Long.class
                                || entry.getValue().getClass() == Float.class
                                || entry.getValue().getClass() == Double.class
                ) {
                    sb.append(entry.getValue());
                } else if (entry.getValue().getClass().isArray()) {
                    sb.append(json2string(arrayToList(entry.getValue())));
                } else if (entry.getValue() instanceof List) {
                    sb.append(json2string((List<?>) entry.getValue()));
                } else if (entry.getValue() instanceof Map) {
                    sb.append(json2string((Map<?, ?>) entry.getValue()));
                } else {
                    sb.append("\"").append(entry.getValue()).append("\"");
                }
            }
        }
        String str = "{" + (sb.length() == 0 ? "" : sb.substring(1)) + "}";
        return str;
    }

    private static String json2string(List<?> json) {
        if (null == json) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Object obj : json) {
            sb.append(",");
            if (null == obj) {
                sb.append((String) null);
            } else if (obj.getClass().isPrimitive()) {
                sb.append(obj);
            } else if (
                    obj.getClass() == Boolean.class
                            || obj.getClass() == Byte.class
                            || obj.getClass() == Short.class
                            || obj.getClass() == Integer.class
                            || obj.getClass() == Long.class
                            || obj.getClass() == Float.class
                            || obj.getClass() == Double.class
            ) {
                sb.append(obj);
            } else if (obj.getClass().isArray()) {
                sb.append(json2string(arrayToList(obj)));
            } else if (obj instanceof List) {
                sb.append(json2string((List<?>) obj));
            } else if (obj instanceof Map) {
                sb.append(json2string((Map<?, ?>) obj));
            } else {
                sb.append("\"").append(obj).append("\"");
            }
        }

        String str = "[" + (sb.length() == 0 ? "" : sb.substring(1)) + "]";
        return str;
    }

    public HttpRequest json(String json) {
        return json(json, CHARSET_UTF8);
    }

    public HttpRequest json(String json, final String charset) {
        contentType(CONTENT_TYPE_JSON, charset);
        try {
            openOutput();
            output.write(json);
            log.debug("[{}]-[{}] json:{}", requestMethod, getConnection().hashCode(), json);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Configure HTTPS connection to trust all certificates
     * <p>
     * This method does nothing if the current request is not a HTTPS request
     *
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest trustAllCerts() throws HttpRequestException {
        final HttpURLConnection connection = getConnection();
        if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection) connection)
                    .setSSLSocketFactory(getTrustedFactory());
        return this;
    }

    /**
     * Configure HTTPS connection to trust all hosts using a custom
     * {@link HostnameVerifier} that always returns <code>true</code> for each
     * host verified
     * <p>
     * This method does nothing if the current request is not a HTTPS request
     *
     * @return this request
     */
    public HttpRequest trustAllHosts() {
        final HttpURLConnection connection = getConnection();
        if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection) connection)
                    .setHostnameVerifier(getTrustedVerifier());
        return this;
    }

    /**
     * Get the {@link URL} of this request's connection
     *
     * @return request URL
     */
    public URL url() {
        return getConnection().getURL();
    }

    /**
     * Get the HTTP method of this request
     *
     * @return method
     */
    public String method() {
        return getConnection().getRequestMethod();
    }

    /**
     * Configure an HTTP proxy on this connection. Use {{@link #proxyBasic(String, String)} if
     * this proxy requires basic authentication.
     *
     * @param proxyHost
     * @param proxyPort
     * @return this request
     */
    public HttpRequest useProxy(final String proxyHost, final int proxyPort) {
        if (connection != null)
            throw new IllegalStateException("The connection has already been created. This method must be called before reading or writing to the request.");

        this.httpProxyHost = proxyHost;
        this.httpProxyPort = proxyPort;
        return this;
    }

    /**
     * Set whether or not the underlying connection should follow redirects in
     * the response.
     *
     * @param followRedirects - true fo follow redirects, false to not.
     * @return this request
     */
    public HttpRequest followRedirects(final boolean followRedirects) {
        getConnection().setInstanceFollowRedirects(followRedirects);
        return this;
    }
}
