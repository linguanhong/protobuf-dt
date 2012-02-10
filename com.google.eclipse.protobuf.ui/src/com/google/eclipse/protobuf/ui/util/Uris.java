/*
 * Copyright (c) 2011 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.protobuf.ui.util;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.*;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.emf.common.util.URI;

import com.google.inject.Singleton;

/**
 * Utility methods related to URIs.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
@Singleton public class Uris {
  public static String PLATFORM_RESOURCE_PREFIX = "platform:/resource";
  public static String FILE_PREFIX = "file:";

  /**
   * Indicates whether the resource or file referred by the given URI exists.
   * @param uri the URI to check.
   * @return {@code true} if the resource or file referred by the given URI exists, {@code false} otherwise.
   */
  public boolean referredResourceExists(URI uri) {
    if (uri.isFile()) {
      File file = new File(uri.path());
      return file.exists();
    }
    if (uri.isPlatformResource()) {
      return referredFileExists(uri);
    }
    return false;
  }

  /**
   * Returns the segments of the given URI without the file name (last segment.)
   * @param uri the give URI.
   * @return the segments of the given URI without the file name (last segment.)
   */
  public List<String> segmentsWithoutFileName(URI uri) {
    List<String> originalSegments = uri.segmentsList();
    if (originalSegments.isEmpty()) {
      return emptyList();
    }
    List<String> segments = newArrayList(originalSegments);
    if (uri.isPlatformResource()) {
      segments.remove(0);
    }
    segments.remove(segments.size() - 1);
    return unmodifiableList(segments);
  }

  /**
   * Returns the "prefix" of the given URI as follows:
   * <ul>
   * <li><code>{@link #PLATFORM_RESOURCE_PREFIX}</code>, if the URI refers to a platform resource</li>
   * <li><code>{@link #FILE_PREFIX}</code>, if the URI refers to a file</li>
   * <li>{@code null} otherwise</li>
   * </ul>
   * @param uri the given URI.
   * @return the "prefix" of the given URI.
   */
  public String prefixOf(URI uri) {
    if (uri.isFile()) {
      return FILE_PREFIX;
    }
    if (uri.isPlatformResource()) {
      return PLATFORM_RESOURCE_PREFIX;
    }
    return "";
  }

  /**
   * Returns the project that contains the file referred by the given URI.
   * @param resourceUri the given URI.
   * @return the project that contains the file referred by the given URI, or {@code null} if the resource referred by
   * the given URI is not a file in the workspace.
   */
  public IProject projectOfReferredFile(URI resourceUri) {
    IFile file = referredFile(resourceUri);
    return (file != null) ? file.getProject() : null;
  }

  /**
   * Indicates whether the given URI refers to an existing file.
   * @param fileUri the URI to check, as a {@code String}.
   * @return {@code true} if the given URI refers to an existing file, {@code false} otherwise.
   */
  public boolean referredFileExists(URI fileUri) {
    IFile file = referredFile(fileUri);
    return (file != null) ? file.exists() : false;
  }

  /**
   * Returns a handle to a workspace file referred by the given URI.
   * @param uri the given URI.
   * @return a handle to a workspace file referred by the given URI or {@code null} if the URI does not refer a
   * workspace file.
   */
  public IFile referredFile(URI uri) {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IPath path = pathOf(uri);
    return (path != null) ? root.getFile(path) : null;
  }

  private IPath pathOf(URI uri) {
    String uriAsText = uri.toPlatformString(true);
    return (uriAsText != null) ? new Path(uriAsText) : null;
  }
}