package io.robe.assets;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Servlet for serving assets with configuration. This is copied from
 * {@link io.dropwizard.servlets.assets.AssetServlet} only loadAsset changed.
 *
 * @see io.dropwizard.servlets.assets.AssetServlet
 */

public class FileAssetServlet extends HttpServlet {
	private static final long serialVersionUID = 6393345594784987908L;
	private static final CharMatcher SLASHES = CharMatcher.is('/');

	private static class CachedAsset {
		private final byte[] resource;
		private final String eTag;
		private final long lastModifiedTime;

		private CachedAsset(byte[] resource, long lastModifiedTime) {
			this.resource = resource;
			this.eTag = '"' + Hashing.murmur3_128().hashBytes(resource).toString() + '"';
			this.lastModifiedTime = lastModifiedTime;
		}

		public byte[] getResource() {
			return resource;
		}

		public String getETag() {
			return eTag;
		}

		public long getLastModifiedTime() {
			return lastModifiedTime;
		}
	}

	private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;

	private final String resourcePath;
	private final String uriPath;
	private final String indexFile;
	private final Charset defaultCharset;

	/**
	 * Creates a new {@code FileAssetServlet} that serves static assets loaded from {@code resourceURL}
	 * (typically a file: or jar: URL). The assets are served at URIs rooted at {@code uriPath}. For
	 * example, given a {@code resourceURL} of {@code "file:/data/assets"} and a {@code uriPath} of
	 * {@code "/js"}, an {@code AssetServlet} would serve the contents of {@code
	 * /data/assets/example.js} in response to a request for {@code /js/example.js}. If a directory
	 * is requested and {@code indexFile} is defined, then {@code AssetServlet} will attempt to
	 * serve a file with that name in that directory. If a directory is requested and {@code
	 * indexFile} is null, it will serve a 404.
	 *
	 * @param resourcePath   the base URL from which assets are loaded
	 * @param uriPath        the URI path fragment in which all requests are rooted
	 * @param indexFile      the filename to use when directories are requested, or null to serve no
	 *                       indexes
	 * @param defaultCharset the default character set
	 */
	public FileAssetServlet(String resourcePath,
	                        String uriPath,
	                        String indexFile,
	                        Charset defaultCharset) {
		this.resourcePath = resourcePath.isEmpty() ? resourcePath : resourcePath + '/';
		final String trimmedUri = SLASHES.trimTrailingFrom(uriPath);
		this.uriPath = trimmedUri.isEmpty() ? "/" : trimmedUri;
		this.indexFile = indexFile;
		this.defaultCharset = defaultCharset;
	}

	public URL getResourceURL() {
		return Resources.getResource(resourcePath);
	}

	public String getUriPath() {
		return uriPath;
	}

	public String getIndexFile() {
		return indexFile;
	}

	@Override
	protected void doGet(HttpServletRequest req,
	                     HttpServletResponse resp) throws ServletException, IOException {
		try {
			final StringBuilder builder = new StringBuilder(req.getServletPath());
			if (req.getPathInfo() != null) {
				builder.append(req.getPathInfo());
			} else {
				builder.insert(0, req.getContextPath());
				builder.append("/").append(getIndexFile());
				resp.sendRedirect(builder.toString());
				return;
			}
			final CachedAsset cachedAsset = loadAsset(builder.toString());
			if (cachedAsset == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			if (isCachedClientSide(req, cachedAsset)) {
				resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}

			resp.setDateHeader(HttpHeaders.LAST_MODIFIED, cachedAsset.getLastModifiedTime());
			resp.setHeader(HttpHeaders.ETAG, cachedAsset.getETag());

			final String mimeTypeOfExtension = req.getServletContext()
					.getMimeType(req.getRequestURI());
			MediaType mediaType = DEFAULT_MEDIA_TYPE;

			if (mimeTypeOfExtension != null) {
				try {
					mediaType = MediaType.parse(mimeTypeOfExtension);
					if (defaultCharset != null && mediaType.is(MediaType.ANY_TEXT_TYPE)) {
						mediaType = mediaType.withCharset(defaultCharset);
					}
				} catch (IllegalArgumentException ignore) {
				}
			}

			resp.setContentType(mediaType.type() + '/' + mediaType.subtype());

			if (mediaType.charset().isPresent()) {
				resp.setCharacterEncoding(mediaType.charset().get().toString());
			}

			try (ServletOutputStream output = resp.getOutputStream()) {
				output.write(cachedAsset.getResource());
			}
		} catch (RuntimeException | URISyntaxException ignored) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private CachedAsset loadAsset(String key) throws URISyntaxException, IOException {
		Preconditions.checkArgument(key.startsWith(uriPath));
		final String requestedResourcePath = SLASHES.trimFrom(key.substring(uriPath.length()));
		final String absoluteRequestedResourcePath = this.resourcePath + requestedResourcePath;

		File requestedResource = new File(absoluteRequestedResourcePath);
		if (!requestedResource.exists()) {
			return null;
		}
		if (requestedResource.isDirectory()) {
			if (indexFile != null) {
				requestedResource = new File(absoluteRequestedResourcePath + '/' + indexFile);
			} else {
				// directory requested but no index file defined
				return null;
			}
		}

		long lastModified = requestedResource.lastModified();
		if (lastModified < 1) {
			// Something went wrong trying to get the last modified time: just use the current time
			lastModified = System.currentTimeMillis();
		}

		// zero out the millis since the date we get back from If-Modified-Since will not have them
		lastModified = (lastModified / 1000) * 1000;
		return new CachedAsset(Files.toByteArray(requestedResource), lastModified);
	}

	private boolean isCachedClientSide(HttpServletRequest req, CachedAsset cachedAsset) {
		return cachedAsset.getETag().equals(req.getHeader(HttpHeaders.IF_NONE_MATCH)) ||
				(req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) >= cachedAsset.getLastModifiedTime());
	}
}