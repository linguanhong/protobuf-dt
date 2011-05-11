/*
 * Copyright (c) 2011 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.protobuf.ui.scoping;

import static com.google.eclipse.protobuf.ui.preferences.paths.FileResolutionType.SINGLE_FOLDER;
import static org.eclipse.emf.common.util.URI.createURI;
import static org.eclipse.xtext.util.Tuples.pair;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreAccess;
import org.eclipse.xtext.util.Pair;

import com.google.eclipse.protobuf.scoping.ImportUriFixer;
import com.google.eclipse.protobuf.scoping.ResourceChecker;
import com.google.eclipse.protobuf.ui.preferences.paths.Preferences;
import com.google.inject.Inject;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class ProtobufImportUriFixer implements ImportUriFixer {
  
  private static final String SEPARATOR = "/";

  @Inject private IPreferenceStoreAccess access;
  
  /*
   * The import URI is relative to the file where the import is. Protoc works fine, but the editor doesn't.
   * In order for the editor to see the import, we need to add to the import URI "platform:resource" and the parent
   * folder of the file containing the import.
   *
   * For example: given the following file hierarchy:
   *
   * - protobuf-test (project)
   *   - folder
   *     - proto2.proto
   *   - proto1.proto
   *
   * If we import "folder/proto2.proto" into proto1.proto, proto1.proto will compile fine, but the editor will complain.
   * We need to have the import URI as "platform:/resource/protobuf-test/folder/proto2.proto" for the editor to see it.
   */
  public String fixUri(String importUri, URI resourceUri, ResourceChecker checker) {
    if (importUri.startsWith(PREFIX)) return importUri;
    Pair<String, List<String>> importUriPair = pair(importUri, createURI(importUri).segmentsList());
    Preferences preferences = Preferences.loadPreferences(access, projectFrom(resourceUri));
    String fixed = fixUri(importUriPair, resourceUri, checker, preferences);
    System.out.println(resourceUri + " : " + importUri + " : " + fixed);
    if (fixed == null) return importUri;
    return fixed;
  }

  private static IProject projectFrom(URI resourceUri) {
    IPath resourcePath = new Path(resourceUri.toPlatformString(true));
    IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);
    return resource.getProject();
  }
  
  private String fixUri(Pair<String, List<String>> importUri, URI resourceUri, ResourceChecker checker,
      Preferences preferences) {
    List<String> segments = removeFirstAndLast(resourceUri.segmentsList());
    if (preferences.fileResolutionType.equals(SINGLE_FOLDER)) {
      return fixUri(importUri, segments, checker);
    }
    List<String> folderNames = preferences.folderNames;
    for (String folderName : folderNames) {
      segments.set(1, folderName);
      String fixed = fixUri(importUri, segments, checker);
      if (fixed != null) return fixed;
    }
    return null;
  }
  
  private List<String> removeFirstAndLast(List<String> list) {
    if (list.isEmpty()) return list;
    List<String> newList = new ArrayList<String>(list);
    newList.remove(0);
    newList.remove(newList.size() - 1);
    return newList;
  }
  
  private String fixUri(Pair<String, List<String>> importUri, List<String> resourceUri, ResourceChecker checker) {
    StringBuilder prefix = new StringBuilder();
    prefix.append(PREFIX);
    String firstSegment = importUri.getSecond().get(0);
    for (String segment : resourceUri) {
      if (segment.equals(firstSegment)) break;
      prefix.append(SEPARATOR).append(segment);
    }
    prefix.append(SEPARATOR);
    String fixed =  prefix.toString() + importUri.getFirst();
    if (checker.resourceExists(fixed)) return fixed;
    return null;
  }
}
